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
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.NoOpResponseParser;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.common.params.CoreAdminParams;
import org.apache.solr.common.util.NamedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class CoreAdminAPIStatusScraper {
    private static final Logger logger = LoggerFactory.getLogger(CoreAdminAPIStatusScraper.class);

    /**
     * Collect core status.
     *
     * @param httpSolrClients
     * @param cores
     * @return
     */
    public List<Collector.MetricFamilySamples> scrape(List<HttpSolrClient> httpSolrClients, List<String> cores) {
        List<Collector.MetricFamilySamples> metricFamilies = new ArrayList<>();

        CoreAdminRequest coreAdminRequest = new CoreAdminRequest();
        coreAdminRequest.setAction(CoreAdminParams.CoreAdminAction.STATUS);
        coreAdminRequest.setIndexInfoNeeded(true);

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
                    coreAdminRequest.setCoreName(core);
                    NamedList<Object> coreAdminResponse = httpSolrClient.request(coreAdminRequest);

                    response.put("baseUrl", httpSolrClient.getBaseURL());
                    response.put("core", core);
                    response.put("responseHeader", JsonPath.read((String) coreAdminResponse.get("response"), "$.responseHeader"));
                    response.put("status", JsonPath.read((String) coreAdminResponse.get("response"), "$.status"));
                } catch (SolrServerException | IOException e) {
                    logger.error("Get status failed: " + e.toString());
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

                String metricNamePrefix = "solr.CoreAdminAPI_STATUS";
                String help = "See following URL: https://lucene.apache.org/solr/guide/6_6/coreadmin-api.html#CoreAdminAPI-STATUSl";
                List<String> labelName = Arrays.asList("baseUrl", "core");
                List<String> labelValue = Arrays.asList(baseUrl, core);

                Map<String, Object> status = JsonPath.read(response.get("status"), "$." + core);
                Map<String,Object> flattenData = new JsonFlattener(mapper.writeValueAsString(status)).withFlattenMode(FlattenMode.NORMAL).flattenAsMap();
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
