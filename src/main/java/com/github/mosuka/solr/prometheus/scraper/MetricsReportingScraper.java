/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.mosuka.solr.prometheus.scraper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mosuka.solr.prometheus.collector.SolrCollector;
import com.github.wnameless.json.flattener.FlattenMode;
import com.github.wnameless.json.flattener.JsonFlattener;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;
import net.minidev.json.JSONArray;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.NoOpResponseParser;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MetricsReportingScraper {
    private static final Logger logger = LoggerFactory.getLogger(MetricsReportingScraper.class);

    /**
     * Collect MetricsReporting Reporting.
     *
     * @param httpSolrClients
     * @return
     */
    public List<Collector.MetricFamilySamples> scrape(List<HttpSolrClient> httpSolrClients, List<String> group, List<String> type, List<String> prefix) {
        List<Collector.MetricFamilySamples> metricFamilies = new ArrayList<>();

        ModifiableSolrParams params = new ModifiableSolrParams();
        params.set("compact", "false");
        if (group.size() > 0) {
            params.set("group", String.join(",", group));
        }
        if (type.size() > 0) {
            params.set("type", String.join(",", type));
        }
        if (prefix.size() > 0) {
            params.set("prefix", String.join(",", prefix));
        }
        QueryRequest metricsRequest = new QueryRequest(params);
        metricsRequest.setPath("/admin/metrics");

        List<Map<String, Object>> responses = new ArrayList<>();

        for (HttpSolrClient httpSolrClient : httpSolrClients) {
            NoOpResponseParser responseParser = new NoOpResponseParser();
            responseParser.setWriterType("json");

            httpSolrClient.setParser(responseParser);

            try {
                NamedList<Object> metricsResponse = httpSolrClient.request(metricsRequest);

                Map<String, Object> response = new LinkedHashMap<>();

                response.put("baseUrl", httpSolrClient.getBaseURL());
                response.put("responseHeader", JsonPath.read((String) metricsResponse.get("response"), "$.responseHeader"));
                response.put("metrics", JsonPath.read((String) metricsResponse.get("response"), "$.metrics"));

                // for Solr 6.5.x
                if (response.get("metrics") instanceof JSONArray) {
                    Map<String, Object> newMetrics = new LinkedHashMap<>();
                    for (int i = 0; i < ((JSONArray) response.get("metrics")).size(); i++) {
                        newMetrics.put((String) ((JSONArray) response.get("metrics")).get(i), ((JSONArray) response.get("metrics")).get(++i));
                    }
                    if (newMetrics.size() > 0) {
                        response.put("metrics", newMetrics);
                    }
                }

                responses.add(response);
            } catch (Exception e) {
                logger.error("Get metrics response failed: " + e.toString());
            }
        }

        ObjectMapper mapper = new ObjectMapper();
        Map<String, GaugeMetricFamily> gaugeMetricFamilies = new LinkedHashMap<>();

        try {
            for (Map<String, Object> response : responses) {
                String metricNamePrefix = "";
                String help = "";
                List<String> labelName = new ArrayList<>();
                List<String> labelValue = new ArrayList<>();

                String baseUrl = (String) response.get("baseUrl");

                Map<String, Object> metrics = (Map<String, Object>) response.get("metrics");
                for (String registryName : metrics.keySet()) {
                    if (registryName.equals("solr.jvm")) {
                        metricNamePrefix = "solr.jvm";
                        help = "See following URL: http://lucene.apache.org/solr/guide/6_6/metrics-reporting.html#jvm-registry-code-solr-jvm-code";
                        labelName = Collections.singletonList("baseUrl");
                        labelValue = Collections.singletonList(baseUrl);
                    } else if(registryName.equals("solr.node")) {
                        metricNamePrefix = "solr.node";
                        help = "See following URL: http://lucene.apache.org/solr/guide/6_6/metrics-reporting.html#node-corecontainer-registry-code-solr-node-code";
                        labelName = Collections.singletonList("baseUrl");
                        labelValue = Collections.singletonList(baseUrl);
                    } else if(registryName.equals("solr.http")) {
                        metricNamePrefix = "solr.http";
                        help = "See following URL: http://lucene.apache.org/solr/guide/6_6/metrics-reporting.html#http-registry-code-solr-http-code";
                        labelName = Collections.singletonList("baseUrl");
                        labelValue = Collections.singletonList(baseUrl);
                    } else if(registryName.equals("solr.jetty")) {
                        metricNamePrefix = "solr.jetty";
                        help = "See following URL: http://lucene.apache.org/solr/guide/6_6/metrics-reporting.html#jetty-registry-code-solr-jetty-code";
                        labelName = Collections.singletonList("baseUrl");
                        labelValue = Collections.singletonList(baseUrl);
                    } else if(registryName.startsWith("solr.core.")) {
                        Pattern pattern = Pattern.compile("(?:[^.]+)\\.(?:[^.]+)\\.(?<collection>[^.]+)(?:|\\.(?<shard>[^.]+)(?:|\\.(?<replica>[^.]+)))$");
                        Matcher matcher = pattern.matcher(registryName);
                        String collection = "";
                        String shard = "";
                        String replica = "";
                        if (matcher.matches()) {
                            collection = matcher.group("collection") != null ? matcher.group("collection") : "";
                            shard = matcher.group("shard") != null ? matcher.group("shard") : "";
                            replica = matcher.group("replica") != null ? matcher.group("replica") : "";
                        }

                        metricNamePrefix = "solr.core";
                        help = "See following URL: http://lucene.apache.org/solr/guide/6_6/metrics-reporting.html#core-solrcore-registry";
                        labelName = Arrays.asList("baseUrl", "collection", "shard", "replica");
                        labelValue = Arrays.asList(baseUrl, collection, shard, replica);
                    }

                    Map<String,Object> flattenData = new JsonFlattener(mapper.writeValueAsString(metrics.get(registryName))).withFlattenMode(FlattenMode.NORMAL).flattenAsMap();
                    for (String key : flattenData.keySet()) {
                        Object value = flattenData.get(key);

                        String metricName = SolrCollector.safeName(String.join("_", metricNamePrefix, key));
                        if (!gaugeMetricFamilies.containsKey(metricName)) {
                            GaugeMetricFamily gauge = new GaugeMetricFamily(
                                    metricName,
                                    help,
                                    labelName);
                            gaugeMetricFamilies.put(metricName, gauge);
                        }

                        if (value instanceof Number) {
                            try {
                                Double metricValue = ((Number) value).doubleValue();
                                gaugeMetricFamilies.get(metricName).addMetric(labelValue, metricValue);
                            } catch (PathNotFoundException e) {
                                logger.warn("Get leader state failed: " + e.toString());
                            }
                        } else {
                            logger.debug("Non numerical value: " + value);
                            continue;
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Collect failed: " + e.toString());
        }

        for (String gaugeMetricName : gaugeMetricFamilies.keySet()) {
            if (gaugeMetricFamilies.get(gaugeMetricName).samples.size() > 0) {
                metricFamilies.add(gaugeMetricFamilies.get(gaugeMetricName));
            }
        }

        return metricFamilies;
    }
}
