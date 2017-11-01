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
package com.github.mosuka.solr.prometheus.collector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mosuka.solr.prometheus.collector.config.Config;
import com.github.mosuka.solr.prometheus.scraper.*;
import com.github.mosuka.solr.prometheus.scraper.config.ScrapeConfig;
import io.prometheus.client.Collector;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.NoOpResponseParser;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.common.params.CoreAdminParams;
import org.apache.solr.common.util.NamedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * SolrCollector
 *
 */
public class SolrCollector extends Collector implements Collector.Describable {
    private static final Logger logger = LoggerFactory.getLogger(SolrCollector.class);

    private SolrClient solrClient;
    private Config config = new Config();

    private static ObjectMapper om = new ObjectMapper();

    /**
     * Constructor.
     *
     * @param config
     */
    public SolrCollector(SolrClient solrClient, Config config) {
        this.solrClient = solrClient;
        this.config = config;
    }

    /**
     * Describe scrape status.
     *
     * @return
     */
    public List<Collector.MetricFamilySamples> describe() {
        List<Collector.MetricFamilySamples> metricFamilies = new ArrayList<>();
        metricFamilies.add(new Collector.MetricFamilySamples("solr_scrape_duration_seconds", Type.GAUGE, "Time this Solr scrape took, in seconds.", new ArrayList<>()));
        return metricFamilies;
    }

    /**
     *
     * @param solrClient
     * @param pingConfig
     * @return
     */
    private Map<String, Collector.MetricFamilySamples> collectPing(SolrClient solrClient, ScrapeConfig pingConfig) {
        Map<String, Collector.MetricFamilySamples> metricFamilySamplesMap = new LinkedHashMap<>();

        SolrScraper scraper = new SolrScraper();

        if (pingConfig.getQuery().getCollection() != null && !pingConfig.getQuery().getCollection().equals("")) {
            // collect specified collection/core
            metricFamilySamplesMap = scraper.collectResponse(solrClient, pingConfig);
        } else {
            // collect all collections/cores
            try {
                List<String> cores = getCores((HttpSolrClient) solrClient);

                for (String core : cores) {
                    try {
                        // clone scrape config
                        ScrapeConfig c = pingConfig.clone();
                        c.getQuery().setCollection(core);
                        mergeMetrics(metricFamilySamplesMap, scraper.collectResponse(solrClient, c));
                    } catch (CloneNotSupportedException e) {
                        logger.error(e.getMessage());
                    }
                }
            } catch (SolrServerException | IOException e) {
                logger.error(e.getMessage());
            }
        }

        return metricFamilySamplesMap;
    }

    /**
     *
     * @param solrClient
     * @param metricsConfig
     * @return
     */
    private Map<String, Collector.MetricFamilySamples> collectMetrics(SolrClient solrClient, ScrapeConfig metricsConfig) {
        SolrScraper scraper = new SolrScraper();

        return scraper.collectResponse(solrClient, metricsConfig);
    }

    /**
     *
     * @param solrClient
     * @param collectionsConfig
     * @return
     */
    private Map<String, Collector.MetricFamilySamples> collectCollections(SolrClient solrClient, ScrapeConfig collectionsConfig) {
        SolrScraper scraper = new SolrScraper();

        return scraper.collectResponse(solrClient, collectionsConfig);
    }

    /**
     *
     * @param solrClient
     * @param queryConfig
     * @return
     */
    private Map<String, Collector.MetricFamilySamples> collectQueries(SolrClient solrClient, ScrapeConfig queryConfig) {
        SolrScraper scraper = new SolrScraper();

        return scraper.collectResponse(solrClient, queryConfig);
    }

    /**
     * Collect samples.
     *
     * @return
     */
    public List<Collector.MetricFamilySamples> collect() {
        // start time of scraping.
        long startTime = System.nanoTime();

        Map<String, Collector.MetricFamilySamples> metricFamilySamplesMap = new LinkedHashMap<>();

        if (this.solrClient instanceof CloudSolrClient) {
            try {
                List<HttpSolrClient> httpSolrClients = getHttpSolrClients((CloudSolrClient) this.solrClient);
                for (HttpSolrClient httpSolrClient : httpSolrClients) {
                    try {
                        mergeMetrics(metricFamilySamplesMap, collectPing(httpSolrClient, this.config.getPingConfig()));
                        mergeMetrics(metricFamilySamplesMap, collectMetrics(httpSolrClient, this.config.getMetricsConfig()));
                    } finally {
                        try {
                            httpSolrClient.close();
                        } catch (IOException e) {
                            logger.error(e.getMessage());
                        }
                    }
                }
            } catch (SolrServerException | IOException e) {
                logger.error(e.getMessage());
            }

            mergeMetrics(metricFamilySamplesMap, collectMetrics(this.solrClient, this.config.getCollectionsConfig()));


        } else {
            mergeMetrics(metricFamilySamplesMap, collectPing(this.solrClient, this.config.getPingConfig()));
            mergeMetrics(metricFamilySamplesMap, collectMetrics(this.solrClient, this.config.getMetricsConfig()));
        }

        for (ScrapeConfig c : config.getQueryConfigs()) {
            mergeMetrics(metricFamilySamplesMap, collectQueries(this.solrClient, c));
        }

        List<MetricFamilySamples> metricFamiliesSamplesList = new ArrayList<>();

        // add solr metrics
        for (String gaugeMetricName : metricFamilySamplesMap.keySet()) {
            Collector.MetricFamilySamples metricFamilySamples = metricFamilySamplesMap.get(gaugeMetricName);

            if (metricFamilySamples.samples.size() > 0) {
                metricFamiliesSamplesList.add(metricFamilySamples);
            }
        }

        // add scrape duration metric
        List<Collector.MetricFamilySamples.Sample> durationSample = new ArrayList<>();
        durationSample.add(new Collector.MetricFamilySamples.Sample("solr_scrape_duration_seconds", new ArrayList<>(), new ArrayList<>(), (System.nanoTime() - startTime) / 1.0E9));
        metricFamiliesSamplesList.add(new Collector.MetricFamilySamples("solr_scrape_duration_seconds", Type.GAUGE, "Time this Solr scrape took, in seconds.", durationSample));

        return metricFamiliesSamplesList;
    }

    /**
     *
     * @param metrics1
     * @param metrics2
     * @return
     */
    private Map<String, Collector.MetricFamilySamples> mergeMetrics(Map<String, Collector.MetricFamilySamples> metrics1, Map<String, Collector.MetricFamilySamples> metrics2) {

        // marge MetricFamilySamples
        for (String k : metrics2.keySet()) {
            if (metrics1.containsKey(k)) {
                for (MetricFamilySamples.Sample sample : metrics2.get(k).samples) {
                    if (!metrics1.get(k).samples.contains(sample)) {
                        metrics1.get(k).samples.add(sample);
                    }
                }
            } else {
                metrics1.put(k, metrics2.get(k));
            }
        }

        return metrics1;
    }


    /**
     * Get target cores via CoreAdminAPI.
     *
     * @param httpSolrClient
     * @return
     */
    public static List<String> getCores(HttpSolrClient httpSolrClient) throws SolrServerException, IOException {
        List<String> cores = new ArrayList<>();

        NoOpResponseParser responseParser = new NoOpResponseParser();
        responseParser.setWriterType("json");

        httpSolrClient.setParser(responseParser);

        CoreAdminRequest coreAdminRequest = new CoreAdminRequest();
        coreAdminRequest.setAction(CoreAdminParams.CoreAdminAction.STATUS);
        coreAdminRequest.setIndexInfoNeeded(false);

        NamedList<Object> coreAdminResponse = httpSolrClient.request(coreAdminRequest);

        JsonNode statusJsonNode = om.readTree((String) coreAdminResponse.get("response")).get("status");

        for (Iterator<JsonNode> i = statusJsonNode.iterator(); i.hasNext(); ) {
            String core = i.next().get("name").textValue();
            if (!cores.contains(core)) {
                cores.add(core);
            }
        }

        return cores;
    }

    /**
     * Get collections via CollectionsAPI.
     *
     * @param cloudSolrClient
     * @return
     * @throws SolrServerException
     * @throws IOException
     */
    public static List<String> getCollections(CloudSolrClient cloudSolrClient) throws SolrServerException, IOException {
        List<String> collections = new ArrayList<>();

        NoOpResponseParser responseParser = new NoOpResponseParser();
        responseParser.setWriterType("json");

        cloudSolrClient.setParser(responseParser);

        CollectionAdminRequest collectionAdminRequest = new CollectionAdminRequest.List();

        NamedList<Object> collectionAdminResponse = cloudSolrClient.request(collectionAdminRequest);
        String jsonResponse = (String) collectionAdminResponse.get("response");

//        collections = JsonPath.read(jsonResponse, "$.collections");

        JsonNode responseJson = om.readTree((String) collectionAdminResponse.get("response"));

        return collections;
    }

    /**
     * Get base urls via CollectionsAPI.
     *
     * @param cloudSolrClient
     * @return
     */
    private List<String> getBaseUrls(CloudSolrClient cloudSolrClient) throws SolrServerException, IOException {
        List<String> baseUrls = new ArrayList<>();

        NoOpResponseParser responseParser = new NoOpResponseParser();
        responseParser.setWriterType("json");

        cloudSolrClient.setParser(responseParser);

        CollectionAdminRequest collectionAdminRequest = new CollectionAdminRequest.ClusterStatus();

        NamedList<Object> collectionAdminResponse = cloudSolrClient.request(collectionAdminRequest);

        List<JsonNode> baseUrlJsonNode = om.readTree((String) collectionAdminResponse.get("response")).findValues("base_url");

        for (Iterator<JsonNode> i = baseUrlJsonNode.iterator(); i.hasNext(); ) {
            String baseUrl = i.next().textValue();
            if (!baseUrls.contains(baseUrl)) {
                baseUrls.add(baseUrl);
            }
        }

        return baseUrls;
    }

    /**
     *
     * @param cloudSolrClient
     * @return
     * @throws SolrServerException
     * @throws IOException
     */
    private List<HttpSolrClient> getHttpSolrClients(CloudSolrClient cloudSolrClient) throws SolrServerException, IOException {
        List<HttpSolrClient> solrClients = new ArrayList<>();

        for (String baseUrl : getBaseUrls(cloudSolrClient)) {
            NoOpResponseParser responseParser = new NoOpResponseParser();
            responseParser.setWriterType("json");

            HttpSolrClient.Builder builder = new HttpSolrClient.Builder();
            builder.withBaseSolrUrl(baseUrl);

            HttpSolrClient httpSolrClient = builder.build();
            httpSolrClient.setParser(responseParser);

            solrClients.add(httpSolrClient);
        }

        return solrClients;
    }

    private static final Pattern unsafeChars = Pattern.compile("[^a-zA-Z0-9:_]");
    private static final Pattern multipleUnderscores = Pattern.compile("__+");
    private static final Pattern startsUnderscores = Pattern.compile("^_+");
    private static final Pattern endsUnderscores = Pattern.compile("_+$");

    /**
     * make safe name for metrics exposition format. See https://prometheus.io/docs/instrumenting/exposition_formats/
     *
     * @param name
     * @return
     */
    public static String safeName(String name) {
        String newName = startsUnderscores.matcher(endsUnderscores.matcher(multipleUnderscores.matcher(unsafeChars.matcher(name).replaceAll("_")).replaceAll("_")).replaceAll("")).replaceAll("");
        return newName;
    }

    /**
     *
     * @param map
     * @return
     */
    public static Map<String, Object> flatten(Map<String, Object> map) {
        return flatten(map, ".");
    }

    /**
     *
     * @param map
     * @param delimiter
     * @return
     */
    public static Map<String, Object> flatten(Map<String, Object> map, String delimiter) {
        Map<String, Object> flattenMap = new LinkedHashMap<>();

        readObj(map, flattenMap, delimiter);

        return flattenMap;
    }

    /**
     *
     * @param obj
     * @param flattenMap
     * @param delimiter
     */
    private static void readObj(Object obj, Map<String, Object> flattenMap, String delimiter) {
        readObj(obj, flattenMap, delimiter, new Stack<>());
    }

    /**
     *
     * @param obj
     * @param flattenMap
     * @param delimiter
     * @param stack
     */
    private static void readObj(Object obj, Map<String, Object> flattenMap, String delimiter, Stack<String> stack) {
        try {
            if (obj instanceof Map) {
                for (Object key : ((Map) obj).keySet()) {
                    stack.push(key.toString());
                    readObj(((Map) obj).get(key), flattenMap, delimiter, stack);
                }
            } else if (obj instanceof List) {
                for (int i = 0; ((List) obj).size() > i; i++) {
                    Object key = String.valueOf(i);
                    stack.push(key.toString());
                    readObj(key, flattenMap, delimiter, stack);
                }
            } else {
                String key = String.join(delimiter, stack.toArray(new String[0]));
                flattenMap.put(key, obj);
            }
            if (!stack.isEmpty()) {
                stack.pop();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
