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
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.NoOpResponseParser;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.common.util.NamedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class CollectionsAPIOverseerStatusScraper {
    private static final Logger logger = LoggerFactory.getLogger(CollectionsAPIOverseerStatusScraper.class);

    /**
     * Collect overseer status.
     *
     * @param cloudSolrClient
     * @return
     */
    public List<Collector.MetricFamilySamples> scrape(CloudSolrClient cloudSolrClient) {
        List<Collector.MetricFamilySamples> metricFamilies = new ArrayList<>();

        NoOpResponseParser responseParser = new NoOpResponseParser();
        responseParser.setWriterType("json");

        cloudSolrClient.setParser(responseParser);

        CollectionAdminRequest collectionAdminRequest = new CollectionAdminRequest.OverseerStatus();

        Map<String, Object> response = new LinkedHashMap<>();
        try {
            NamedList<Object> collectionAdminResponse = cloudSolrClient.request(collectionAdminRequest);

            response = JsonPath.read((String) collectionAdminResponse.get("response"), "$");

            if (response.get("overseer_operations") instanceof JSONArray) {
                Map<String, Object> newOverseerStatus = new LinkedHashMap<>();
                for (int i = 0; i < ((JSONArray) response.get("overseer_operations")).size(); i++) {
                    newOverseerStatus.put((String) ((JSONArray) response.get("overseer_operations")).get(i), ((JSONArray) response.get("overseer_operations")).get(++i));
                }
                if (newOverseerStatus.size() > 0) {
                    response.put("overseer_operations", newOverseerStatus);
                }
            }

            if (response.get("collection_operations") instanceof JSONArray) {
                Map<String, Object> newOverseerStatus = new LinkedHashMap<>();
                for (int i = 0; i < ((JSONArray) response.get("collection_operations")).size(); i++) {
                    newOverseerStatus.put((String) ((JSONArray) response.get("collection_operations")).get(i), ((JSONArray) response.get("collection_operations")).get(++i));
                }
                if (newOverseerStatus.size() > 0) {
                    response.put("collection_operations", newOverseerStatus);
                }
            }

            if (response.get("overseer_queue") instanceof JSONArray) {
                Map<String, Object> newOverseerStatus = new LinkedHashMap<>();
                for (int i = 0; i < ((JSONArray) response.get("overseer_queue")).size(); i++) {
                    newOverseerStatus.put((String) ((JSONArray) response.get("overseer_queue")).get(i), ((JSONArray) response.get("overseer_queue")).get(++i));
                }
                if (newOverseerStatus.size() > 0) {
                    response.put("overseer_queue", newOverseerStatus);
                }
            }

            if (response.get("overseer_internal_queue") instanceof JSONArray) {
                Map<String, Object> newOverseerStatus = new LinkedHashMap<>();
                for (int i = 0; i < ((JSONArray) response.get("overseer_internal_queue")).size(); i++) {
                    newOverseerStatus.put((String) ((JSONArray) response.get("overseer_internal_queue")).get(i), ((JSONArray) response.get("overseer_internal_queue")).get(++i));
                }
                if (newOverseerStatus.size() > 0) {
                    response.put("overseer_internal_queue", newOverseerStatus);
                }
            }

            if (response.get("collection_queue") instanceof JSONArray) {
                Map<String, Object> newOverseerStatus = new LinkedHashMap<>();
                for (int i = 0; i < ((JSONArray) response.get("collection_queue")).size(); i++) {
                    newOverseerStatus.put((String) ((JSONArray) response.get("collection_queue")).get(i), ((JSONArray) response.get("collection_queue")).get(++i));
                }
                if (newOverseerStatus.size() > 0) {
                    response.put("collection_queue", newOverseerStatus);
                }
            }
        } catch (SolrServerException | IOException e) {
            logger.error("Get overseer status failed: " + e.toString());
        }

        // leader
        String leader = (String) response.get("leader");

        ObjectMapper mapper = new ObjectMapper();
        Map<String, GaugeMetricFamily> gaugeMetricFamilies = new LinkedHashMap<>();

        try {
            String metricNamePrefix = "solr.CollectionsAPI_OVERSEERSTATUS";
            String help = "See following URL: https://lucene.apache.org/solr/guide/6_6/collections-api.html#CollectionsAPI-overseerstatus";
            List<String> labelName = Arrays.asList("leader");
            List<String> labelValue = Arrays.asList(leader);

            Map<String,Object> flattenData = new JsonFlattener(mapper.writeValueAsString(response)).withFlattenMode(FlattenMode.NORMAL).flattenAsMap();
            for (String k : flattenData.keySet()) {
                if (k.startsWith("responseHeader") || k.startsWith("leader")) {
                    continue;
                }

                String metricName = SolrCollector.safeName(String.join("_", metricNamePrefix, k));
                if (!gaugeMetricFamilies.containsKey(metricName)) {
                    GaugeMetricFamily gauge = new GaugeMetricFamily(
                            metricName,
                            help,
                            labelName);
                    gaugeMetricFamilies.put(metricName, gauge);
                }

                Object value = flattenData.get(k);
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
