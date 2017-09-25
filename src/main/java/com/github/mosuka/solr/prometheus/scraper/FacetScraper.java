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
import com.github.mosuka.solr.prometheus.scraper.config.Query;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;
import net.minidev.json.JSONArray;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.NoOpResponseParser;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class FacetScraper {
    private static final Logger logger = LoggerFactory.getLogger(FacetScraper.class);

    /**
     * Collect facet count.
     *
     * @param solrClient
     * @param queries
     * @return
     */
    public List<Collector.MetricFamilySamples> collectFacet(SolrClient solrClient, List<Query> queries) {
        List<Collector.MetricFamilySamples> metricFamilies = new ArrayList<>();

        List<Map<String, Object>> responses = new ArrayList<>();

        for (Query query : queries) {
            ModifiableSolrParams params = new ModifiableSolrParams();
            for (Map<String, String> param : query.getParams()) {
                for (String name : param.keySet()) {
                    try {
                        params.add(name, param.get(name));
                    } catch (Exception e) {
                        logger.error("Add param failed: " + e.toString());
                    }
                }
            }

            QueryRequest queryRequest = new QueryRequest(params);
            queryRequest.setPath(query.getPath());

            Map<String, Object> response = new LinkedHashMap<>();
            try {
                response.put("queryCollection", query.getCollection());
                response.put("queryPath", query.getPath());
                response.put("queryParams", query.getParamsString());

                NamedList<Object> queryResponse = solrClient.request(queryRequest, query.getCollection());

                response.put("responseHeader", JsonPath.read((String) queryResponse.get("response"), "$.responseHeader"));
                response.put("response", JsonPath.read((String) queryResponse.get("response"), "$.response"));
                response.put("facet_counts", JsonPath.read((String) queryResponse.get("response"), "$.facet_counts"));
            } catch (HttpSolrClient.RemoteSolrException | SolrServerException | IOException e) {
                logger.error("Get facet failed: " + e.toString());
            } finally {
                responses.add(response);
            }
        }

        Map<String, GaugeMetricFamily> gaugeMetricFamilies = new LinkedHashMap<>();

        try {
            for (Map<String, Object> response : responses) {
                String metricNamePrefix = "solr.faceting.facet_counts";
                String help = "See following URL: https://lucene.apache.org/solr/guide/6_6/faceting.html";

                // facet_queries
                try {
                    Map<String, Object> facetQueries = JsonPath.read(response, "$.facet_counts.facet_queries");
                    if (!facetQueries.isEmpty()) {
                        String metricName = SolrCollector.safeName(String.join("_", metricNamePrefix, "facet_queries"));
                        List<String> labelName = Arrays.asList("collection", "query");

                        if (!gaugeMetricFamilies.containsKey(metricName)) {
                            GaugeMetricFamily gauge = new GaugeMetricFamily(
                                    metricName,
                                    help,
                                    labelName);
                            gaugeMetricFamilies.put(metricName, gauge);
                        }

                        for (String facetQuery : facetQueries.keySet()) {
                            Number count = (Number) facetQueries.get(facetQuery);

                            List<String> labelValue = Arrays.asList((String) response.get("queryCollection"), facetQuery);
                            Double metricValue = count.doubleValue();

                            gaugeMetricFamilies.get(metricName).addMetric(labelValue, metricValue);
                        }
                    }
                } catch (PathNotFoundException e) {
                    logger.warn("Get facet queries failed: " + e.toString());
                }

                // facet_fields
                try {
                    Map<String, Object> facetFields = JsonPath.read(response, "$.facet_counts.facet_fields");
                    for (String fieldName : facetFields.keySet()) {
                        String metricName = SolrCollector.safeName(String.join("_", metricNamePrefix, "facet_fields"));
                        List<String> labelName = Arrays.asList("collection", "field", "term");

                        if (!gaugeMetricFamilies.containsKey(metricName)) {
                            GaugeMetricFamily gauge = new GaugeMetricFamily(
                                    metricName,
                                    help,
                                    labelName);
                            gaugeMetricFamilies.put(metricName, gauge);
                        }

                        JSONArray facetField = (JSONArray) facetFields.get(fieldName);
                        for (int i = 0 ; i < facetField.size(); i++) {
                            String term = (String) facetField.get(i);
                            Number count = (Number) facetField.get(++i);

                            List<String> labelValue = Arrays.asList((String) response.get("queryCollection"), fieldName, term);
                            Double metricValue = count.doubleValue();

                            gaugeMetricFamilies.get(metricName).addMetric(labelValue, metricValue);
                        }
                    }
                } catch (PathNotFoundException e) {
                    logger.warn("Get facet fields failed: " + e.toString());
                }

                // facet_ranges
                try {
                    Map<String, Object> facetRanges = JsonPath.read(response, "$.facet_counts.facet_ranges");
                    for (String fieldName : facetRanges.keySet()) {
                        String metricName = SolrCollector.safeName(String.join("_", metricNamePrefix, "facet_ranges"));
                        List<String> labelName = Arrays.asList("collection", "field", "range");

                        if (!gaugeMetricFamilies.containsKey(metricName)) {
                            GaugeMetricFamily gauge = new GaugeMetricFamily(
                                    metricName,
                                    help,
                                    labelName);
                            gaugeMetricFamilies.put(metricName, gauge);
                        }

                        Map<String, Object> facetRange = (Map<String, Object>) facetRanges.get(fieldName);
                        JSONArray facetValues = (JSONArray) facetRange.get("counts");
                        for (int i = 0 ; i < facetValues.size(); i++) {
                            String range = (String) facetValues.get(i);
                            Number count = (Number) facetValues.get(++i);

                            List<String> labelValue = Arrays.asList((String) response.get("queryCollection"), fieldName, range);
                            Double metricValue = count.doubleValue();

                            gaugeMetricFamilies.get(metricName).addMetric(labelValue, metricValue);
                        }
                    }
                } catch (PathNotFoundException e) {
                    logger.warn("Get facet range failed: " + e.toString());
                }

                // facet_intervals
                try {
                    Map<String, Object> facetIntervals = JsonPath.read(response, "$.facet_counts.facet_intervals");
                    for (String fieldName : facetIntervals.keySet()) {
                        String metricName = SolrCollector.safeName(String.join("_", metricNamePrefix, "facet_intervals"));
                        List<String> labelName = Arrays.asList("collection", "field", "interval");

                        if (!gaugeMetricFamilies.containsKey(metricName)) {
                            GaugeMetricFamily gauge = new GaugeMetricFamily(
                                    metricName,
                                    help,
                                    labelName);
                            gaugeMetricFamilies.put(metricName, gauge);
                        }

                        Map<String, Object> facetInterval = (Map<String, Object>) facetIntervals.get(fieldName);
                        for (String interval : facetInterval.keySet()) {
                            Number count = (Number) facetInterval.get(interval);

                            List<String> labelValue = Arrays.asList((String) response.get("queryCollection"), fieldName, interval);
                            Double metricValue = count.doubleValue();

                            gaugeMetricFamilies.get(metricName).addMetric(labelValue, metricValue);
                        }
                    }
                } catch (PathNotFoundException e) {
                    logger.warn("Get facet intervals failed: " + e.toString());
                }

                // facet_heatmaps
                // TODO
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
