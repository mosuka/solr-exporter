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
                    response.put("responseHeader", JsonPath.read((String) mbeansResponse.get("response"), "$.responseHeader"));
                    response.put("solr-mbeans", JsonPath.read((String) mbeansResponse.get("response"), "$.solr-mbeans"));

                    if (response.get("solr-mbeans") instanceof JSONArray) {
                        Map<String, Object> newSolrMBeans = new LinkedHashMap<>();
                        for (int i = 0; i < ((JSONArray) response.get("solr-mbeans")).size(); i++) {
                            newSolrMBeans.put((String) ((JSONArray) response.get("solr-mbeans")).get(i), ((JSONArray) response.get("solr-mbeans")).get(++i));
                        }
                        if (newSolrMBeans.size() > 0) {
                            response.put("solr-mbeans", newSolrMBeans);
                        }
                    }
                } catch (Exception e) {
                    logger.error("Get mbeans response failed: " + e.toString());
                } finally {
                    responses.add(response);
                }
            }
        }

        ObjectMapper mapper = new ObjectMapper();
        Map<String, GaugeMetricFamily> gaugeMetricFamilies = new LinkedHashMap<>();

        try {
            for (Map<String, Object> response : responses) {
                String baseUrl = (String) response.get("baseUrl");
                String core = (String) response.get("core");

                String metricNamePrefix = "solr.MBeanRequestHandler";
                String help= "See following URL: https://lucene.apache.org/solr/guide/6_6/mbean-request-handler.html";;
                List<String> labelName = Arrays.asList("baseUrl", "core");
                List<String> labelValue = Arrays.asList(baseUrl, core);

                Map<String, Object> solrMBeans = (Map<String, Object>) response.get("solr-mbeans");
                for (String cat : solrMBeans.keySet()) {
                    if (cat.equals("CORE")) {
                        for (Iterator<String> i = ((Map<String, Object>) solrMBeans.get(cat)).keySet().iterator(); i.hasNext();) {
                            if (i.next().startsWith("Searcher@")) {
                                i.remove();
                            }
                        }
                    }

                    Map<String,Object> flattenData = new JsonFlattener(mapper.writeValueAsString(solrMBeans.get(cat))).withFlattenMode(FlattenMode.NORMAL).flattenAsMap();
                    for (String k : flattenData.keySet()) {
                        Object value = flattenData.get(k);

                        String metricName = SolrCollector.safeName(String.join("_", metricNamePrefix, cat, k));

                        // TODO
                        /*
                          "update":{
                            "class":"org.apache.solr.handler.UpdateRequestHandlerApi",
                            "version":"6.6.0",
                            "description":"Add documents using XML (with XSLT), CSV, JSON, or javabin",
                            "src":null,
                            "stats":{
                              "handlerStart":1506346609347,
                              "requests":0,
                              "errors":0,
                              "serverErrors":0,
                              "clientErrors":0,
                              "timeouts":0,
                              "totalTime":0.0,
                              "avgRequestsPerSecond":0.0,
                              "5minRateRequestsPerSecond":0.0,
                              "15minRateRequestsPerSecond":0.0,
                              "avgTimePerRequest":0.0,
                              "medianRequestTime":0.0,
                              "75thPcRequestTime":0.0,
                              "95thPcRequestTime":0.0,
                              "99thPcRequestTime":0.0,
                              "999thPcRequestTime":0.0}},
                          "/update":{
                            "class":"org.apache.solr.handler.UpdateRequestHandler",
                            "version":"6.6.0",
                            "description":"Add documents using XML (with XSLT), CSV, JSON, or javabin",
                            "src":null,
                            "stats":{
                              "handlerStart":1506346609207,
                              "requests":12,
                              "errors":0,
                              "serverErrors":0,
                              "clientErrors":0,
                              "timeouts":0,
                              "totalTime":5067.146268,
                              "avgRequestsPerSecond":0.0015679707990073847,
                              "5minRateRequestsPerSecond":2.122182846349127E-12,
                              "15minRateRequestsPerSecond":5.010074741181942E-6,
                              "avgTimePerRequest":413.2263435499766,
                              "medianRequestTime":12.361623,
                              "75thPcRequestTime":849.2176,
                              "95thPcRequestTime":1844.622575,
                              "99thPcRequestTime":1844.622575,
                              "999thPcRequestTime":1844.622575}}},
                         */

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
