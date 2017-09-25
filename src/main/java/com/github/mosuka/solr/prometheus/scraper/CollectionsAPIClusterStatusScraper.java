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
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.NoOpResponseParser;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.common.util.NamedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class CollectionsAPIClusterStatusScraper {
    private static final Logger logger = LoggerFactory.getLogger(CollectionsAPIClusterStatusScraper.class);

    /**
     * Collect cluster status.
     *
     * @param cloudSolrClient
     * @param collections
     * @return
     */
    public List<Collector.MetricFamilySamples> scrape(CloudSolrClient cloudSolrClient, List<String> collections) {
        List<Collector.MetricFamilySamples> metricFamilies = new ArrayList<>();

        // response list
        List<Map<String, Object>> responses = new ArrayList<>();

        // target collections
        List<String> targetCollections = new ArrayList<>(collections);
        if (collections.isEmpty()) {
            try {
                targetCollections = new ArrayList<>(SolrCollector.getCollections(cloudSolrClient));
            } catch (SolrServerException | IOException e) {
                logger.error("Get collections failed: " + e.toString());
            }
        }

        NoOpResponseParser responseParser = new NoOpResponseParser();
        responseParser.setWriterType("json");

        cloudSolrClient.setParser(responseParser);

        for (String collection : targetCollections) {
            try {
                CollectionAdminRequest collectionAdminRequest = new CollectionAdminRequest.ClusterStatus().setCollectionName(collection);

                NamedList<Object> collectionAdminResponse = cloudSolrClient.request(collectionAdminRequest, collection);

                Map<String, Object> response = JsonPath.read((String) collectionAdminResponse.get("response"), "$");
                responses.add(response);
            } catch (SolrServerException | IOException e) {
                logger.error("Get cluster status failed: " + e.toString());
            }
        }

        Map<String, GaugeMetricFamily> gaugeMetricFamilies = new LinkedHashMap<>();

        String metricNamePrefix = "solr.CollectionsAPI_CLUSTERSTATUS";
        String metricName;
        String help;
        List<String> labelName;
        List<String> labelValue;

        try {
            for (Map<String, Object> response : responses) {
                Map<String, Object> collectionsMap = JsonPath.read(response, "$.cluster.collections");
                for (String collection : collectionsMap.keySet()) {
                    Map<String, Object> collectionMap = (Map<String, Object>) collectionsMap.get(collection);

                    // replicationFactor
                    metricName = SolrCollector.safeName(String.join("_", metricNamePrefix, "replicationFactor"));
                    help = "See following URL: https://lucene.apache.org/solr/guide/6_6/collections-api.html#CollectionsAPI-clusterstatus";
                    labelName = Collections.singletonList("collection");
                    labelValue = Collections.singletonList(collection);
                    if (!gaugeMetricFamilies.containsKey(metricName)) {
                        GaugeMetricFamily gauge = new GaugeMetricFamily(
                                metricName,
                                help,
                                labelName);
                        gaugeMetricFamilies.put(metricName, gauge);
                    }
                    if (collectionMap.containsKey("replicationFactor")) {
                        Double metricValue = Double.parseDouble((String) collectionMap.get("replicationFactor"));
                        gaugeMetricFamilies.get(metricName).addMetric(labelValue, metricValue);
                    }

                    // maxShardsPerNode
                    metricName = SolrCollector.safeName(String.join("_", metricNamePrefix, "maxShardsPerNode"));
                    help = "See following URL: https://lucene.apache.org/solr/guide/6_6/collections-api.html#CollectionsAPI-clusterstatus";
                    labelName = Collections.singletonList("collection");
                    labelValue = Collections.singletonList(collection);
                    if (!gaugeMetricFamilies.containsKey(metricName)) {
                        GaugeMetricFamily gauge = new GaugeMetricFamily(
                                metricName,
                                help,
                                labelName);
                        gaugeMetricFamilies.put(metricName, gauge);
                    }
                    if (collectionMap.containsKey("maxShardsPerNode")) {
                        Double metricValue = Double.parseDouble((String) collectionMap.get("maxShardsPerNode"));
                        gaugeMetricFamilies.get(metricName).addMetric(labelValue, metricValue);
                    }

                    // znodeVersion
                    metricName = SolrCollector.safeName(String.join("_", metricNamePrefix, "znodeVersion"));
                    help = "See following URL: https://lucene.apache.org/solr/guide/6_6/collections-api.html#CollectionsAPI-clusterstatus";
                    labelName = Collections.singletonList("collection");
                    labelValue = Collections.singletonList(collection);
                    if (!gaugeMetricFamilies.containsKey(metricName)) {
                        GaugeMetricFamily gauge = new GaugeMetricFamily(
                                metricName,
                                help,
                                labelName);
                        gaugeMetricFamilies.put(metricName, gauge);
                    }
                    if (collectionMap.containsKey("znodeVersion")) {
                        Double metricValue = ((Number) collectionMap.get("znodeVersion")).doubleValue();
                        gaugeMetricFamilies.get(metricName).addMetric(labelValue, metricValue);
                    }

                    if (collectionMap.containsKey("shards")) {
                        Map<String, Object> shardsMap = (Map<String, Object>) collectionMap.get("shards");
                        for (String shard : shardsMap.keySet()) {
                            Map<String, Object> shardMap = (Map<String, Object>) shardsMap.get(shard);

                            // shard state
                            metricName = SolrCollector.safeName(String.join("_", metricNamePrefix, "shard_state"));
                            help = "The shard state (active : 1.0, other : 0.0). See following URL: https://lucene.apache.org/solr/guide/6_6/collections-api.html#CollectionsAPI-clusterstatus";
                            labelName = Arrays.asList("collection", "shard");
                            labelValue = Arrays.asList(collection, shard);
                            if (!gaugeMetricFamilies.containsKey(metricName)) {
                                GaugeMetricFamily gauge = new GaugeMetricFamily(
                                        metricName,
                                        help,
                                        labelName);
                                gaugeMetricFamilies.put(metricName, gauge);
                            }
                            Double metricValue = (shardMap.get("state")).equals("active") ? 1.0 : 0.0;
                            gaugeMetricFamilies.get(metricName).addMetric(labelValue, metricValue);

                            Map<String, Object> replicasMap = (Map<String, Object>) shardMap.get("replicas");
                            for (String replica : replicasMap.keySet()) {
                                Map<String, Object> replicaMap = (Map<String, Object>) replicasMap.get(replica);

                                String core = replicaMap.containsKey("core") ? (String) replicaMap.get("core") : "";
                                String baseUrl = replicaMap.containsKey("base_url") ? (String) replicaMap.get("base_url") : "";
                                String nodeName = replicaMap.containsKey("node_name") ? (String) replicaMap.get("node_name") : "";

                                // replica state
                                metricName = SolrCollector.safeName(String.join("_", metricNamePrefix, "replica_state"));
                                help = "The replica state (acrive : 5.0, recovering : 4.0, down : 3.0, recovery_failed : 2.0, inactive : 1.0, gone : 0.0). See following URL: https://lucene.apache.org/solr/guide/6_6/collections-api.html#CollectionsAPI-clusterstatus";
                                labelName = Arrays.asList("collection", "shard", "replica", "core", "baseUrl", "nodeName");
                                labelValue = Arrays.asList(collection, shard, replica, core, baseUrl, nodeName);
                                if (!gaugeMetricFamilies.containsKey(metricName)) {
                                    GaugeMetricFamily gauge = new GaugeMetricFamily(
                                            metricName,
                                            help,
                                            labelName);
                                    gaugeMetricFamilies.put(metricName, gauge);
                                }
                                String replicaState = (String) replicaMap.get("state");
                                metricValue = 0.0;
                                if (replicaState.equals("active")) {
                                    metricValue = 5.0;
                                } else if (replicaState.equals("recovering")) {
                                    metricValue = 4.0;
                                } else if (replicaState.equals("down")) {
                                    metricValue = 3.0;
                                } else if (replicaState.equals("recovery_failed")) {
                                    metricValue = 2.0;
                                } else if (replicaState.equals("inactive")) {
                                    metricValue = 1.0;
                                } else if (replicaState.equals("down")) {
                                    metricValue = 0.0;
                                }
                                gaugeMetricFamilies.get(metricName).addMetric(labelValue, metricValue);

                                // leader state
                                metricName = SolrCollector.safeName(String.join("_", metricNamePrefix, "leader_state"));
                                help = "The leader state (true : 1.0, false : 0.0). See following URL: https://lucene.apache.org/solr/guide/6_6/collections-api.html#CollectionsAPI-clusterstatus";
                                labelName = Arrays.asList("collection", "shard", "replica", "core", "baseUrl", "nodeName");
                                labelValue = Arrays.asList(collection, shard, replica, core, baseUrl, nodeName);
                                if (!gaugeMetricFamilies.containsKey(metricName)) {
                                    GaugeMetricFamily gauge = new GaugeMetricFamily(
                                            metricName,
                                            help,
                                            labelName);
                                    gaugeMetricFamilies.put(metricName, gauge);
                                }
                                metricValue = replicaMap.containsKey("leader") && Boolean.parseBoolean((String) replicaMap.get("leader")) ? 1.0 : 0.0;
                                gaugeMetricFamilies.get(metricName).addMetric(labelValue, metricValue);
                            }
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
