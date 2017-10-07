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

import com.github.mosuka.solr.prometheus.collector.SolrCollector;
import com.jayway.jsonpath.JsonPath;
import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;
import net.minidev.json.JSONArray;
import org.apache.solr.client.solrj.SolrServerException;
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

public class MBeanRequestHandlerScraper {
    private static final Logger logger = LoggerFactory.getLogger(MBeanRequestHandlerScraper.class);

    /**
     * Collect MBean stats.
     *
     * @param httpSolrClients
     * @param cores
     * @return
     */
    public List<Collector.MetricFamilySamples> scrape(List<HttpSolrClient> httpSolrClients, List<String> cores, String category, String key) {
        List<Collector.MetricFamilySamples> metricFamilies = new ArrayList<>();

        ModifiableSolrParams params = new ModifiableSolrParams();
        params.set("stats", "true");
        if (category != null && !category.equals("")) {
            params.set("category", String.join(",", category));
        }
        if (key != null && !key.equals("")) {
            params.set("key", String.join(",", key));
        }

        QueryRequest mbeansRequest = new QueryRequest(params);
        mbeansRequest.setPath("/admin/mbeans");

        List<Map<String, Object>> responses = new ArrayList<>();

        for (HttpSolrClient httpSolrClient : httpSolrClients) {
            List<String> targetCores = new ArrayList<>(cores);
            if (cores.isEmpty()) {
                try {
                    targetCores = new ArrayList<>(SolrCollector.getCores(httpSolrClient));
                } catch (SolrServerException | IOException e) {
                    logger.error("Get cores failed: " + e.toString());
                }
            }

            NoOpResponseParser responseParser = new NoOpResponseParser();
            responseParser.setWriterType("json");

            httpSolrClient.setParser(responseParser);

            for (String core : targetCores) {
                Map<String, Object> response = new LinkedHashMap<>();
                try {
                    NamedList<Object> mbeansResponse = httpSolrClient.request(mbeansRequest, core);

                    response.put("baseUrl", httpSolrClient.getBaseURL());
                    response.put("core", core);
                    response.put("response", JsonPath.read((String) mbeansResponse.get("response"), "$"));
                    if (((Map<String, Object>) response.get("response")).get("solr-mbeans") instanceof JSONArray) {
                        Map<String, Object> newSolrMBeans = new LinkedHashMap<>();
                        for (int i = 0; i < ((JSONArray) ((Map<String, Object>) response.get("response")).get("solr-mbeans")).size(); i++) {
                            newSolrMBeans.put(
                                    (String) ((JSONArray) ((Map<String, Object>) response.get("response")).get("solr-mbeans")).get(i),
                                    ((JSONArray) ((Map<String, Object>) response.get("response")).get("solr-mbeans")).get(++i)
                            );
                        }
                        if (newSolrMBeans.size() > 0) {
                            ((Map<String, Object>) response.get("response")).put("solr-mbeans", newSolrMBeans);
                        }
                    }
                } catch (Exception e) {
                    logger.error("Get mbeans response failed: " + e.toString());
                } finally {
                    responses.add(response);
                }
            }
        }

        Map<String, GaugeMetricFamily> gaugeMetricFamilies = new LinkedHashMap<>();

        try {
            for (Map<String, Object> response : responses) {
                String baseUrl = (String) response.get("baseUrl");
                String core = (String) response.get("core");

                String metricNamePrefix = "solr.MBeanRequestHandler";

                Map<String, Object> flattenResponse = SolrCollector.flatten((Map<String, Object>) ((Map<String, Object>) response.get("response")).get("solr-mbeans"), "|");
                for (String flattenKey : flattenResponse.keySet()) {
                    logger.debug("flattenKey: " + flattenKey);

                    if (flattenKey.startsWith("CORE|Searcher@")) {
                        // Unnecessary key
                        logger.debug("skip: " + flattenKey);
                        continue;
                    }

                    Pattern pattern = Pattern.compile("(?<category>[^|]+)\\|(?<key>[^|]++)\\|(?:|[^|]+\\|)(?<item>[^|]+)$");
                    Matcher matcher = pattern.matcher(flattenKey);
                    if (matcher.matches()) {
                        String metricCategory = matcher.group("category") != null ? matcher.group("category") : "";
                        String metricKey = matcher.group("key") != null ? matcher.group("key") : "";
                        String metricItem = matcher.group("item") != null ? matcher.group("item") : "";

                        Object value = flattenResponse.get(flattenKey);

                        String metricName = SolrCollector.safeName(String.join("_", metricNamePrefix, metricCategory, metricItem));
                        String help = "See following URL: https://lucene.apache.org/solr/guide/6_6/mbean-request-handler.html";
                        List<String> labelName = Arrays.asList("baseUrl", "core", "key");
                        List<String> labelValue = Arrays.asList(baseUrl, core, metricKey);
                        Double metricValue = null;
                        if (value instanceof Number) {
                            metricValue = ((Number) value).doubleValue();
                        }

                        if (metricValue == null) {
                            continue;
                        }

                        if (!gaugeMetricFamilies.containsKey(metricName)) {
                            GaugeMetricFamily gauge = new GaugeMetricFamily(
                                    metricName,
                                    help,
                                    labelName);
                            gaugeMetricFamilies.put(metricName, gauge);
                            logger.debug("Create metric family: " + gauge.toString());
                        }
                        gaugeMetricFamilies.get(metricName).addMetric(labelValue, metricValue);
                        logger.debug("Add metric: " + gaugeMetricFamilies.get(metricName).toString());
                    } else {
                        logger.debug("skip: " + flattenKey);
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
