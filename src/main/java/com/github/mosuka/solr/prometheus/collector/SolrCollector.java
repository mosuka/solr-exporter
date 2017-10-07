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

import com.github.mosuka.solr.prometheus.collector.config.Config;
import com.github.mosuka.solr.prometheus.scraper.*;
import com.jayway.jsonpath.JsonPath;
import io.prometheus.client.Collector;
import net.minidev.json.JSONArray;
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
    public List<MetricFamilySamples> describe() {
        List<MetricFamilySamples> metricFamilies = new ArrayList<>();
        metricFamilies.add(new MetricFamilySamples("solr_scrape_duration_seconds", Type.GAUGE, "Time this Solr scrape took, in seconds.", new ArrayList<>()));
        return metricFamilies;
    }

    /**
     * Collect samples.
     *
     * @return
     */
    public List<MetricFamilySamples> collect() {
        // start time of scraping.
        long startTime = System.nanoTime();

        List<MetricFamilySamples> metricFamilies = new ArrayList<>();

        if (this.solrClient instanceof CloudSolrClient) {
            // collect overseer status via CollectionsAPI
            if (config.getCollectionsAPIOverseerStatus().getEnable()) {
                CollectionsAPIOverseerStatusScraper collectionsAPIOverseerStatusScraper = new CollectionsAPIOverseerStatusScraper();
                metricFamilies.addAll(collectionsAPIOverseerStatusScraper.scrape((CloudSolrClient) this.solrClient));
            }

            // cluster status via CollectionsAPI
            if (config.getCollectionsAPIClusterStatus().getEnable()) {
                CollectionsAPIClusterStatusScraper collectionsAPIClusterStatusScraper = new CollectionsAPIClusterStatusScraper();
                metricFamilies.addAll(collectionsAPIClusterStatusScraper.scrape((CloudSolrClient) this.solrClient, config.getCollectionsAPIClusterStatus().getCollections()));
            }

            // create target base urls
            List<HttpSolrClient> httpSolrClients = new ArrayList<>();
            try {
                httpSolrClients = getHttpSolrClients((CloudSolrClient) this.solrClient);

                // collect ping status via Ping
                if (config.getPing().getEnable()) {
                    PingScraper pingScraper = new PingScraper();
                    metricFamilies.addAll(pingScraper.scrape(httpSolrClients, config.getPing().getCores()));
                }

                // collect cores status via CoreAdminAPI
                if (config.getCoreAdminAPIStatus().getEnable()) {
                    CoreAdminAPIStatusScraper coreAdminStatusCollector = new CoreAdminAPIStatusScraper();
                    metricFamilies.addAll(coreAdminStatusCollector.scrape(httpSolrClients, config.getCoreAdminAPIStatus().getCores()));
                }

                // collect MBean stats via MBean Query Handler
                if (config.getmBeanRequestHandler().getEnable()) {
                    MBeanRequestHandlerScraper mBeanRequestHandlerScraper = new MBeanRequestHandlerScraper();
                    metricFamilies.addAll(mBeanRequestHandlerScraper.scrape(httpSolrClients, config.getmBeanRequestHandler().getCores(), config.getmBeanRequestHandler().getCat(), config.getmBeanRequestHandler().getKey()));
                }

                // collect metrics via MetricsReporting Reporting
                if (config.getMetricsReporting().getEnable()) {
                    MetricsReportingScraper metricsReportingScraper = new MetricsReportingScraper();
                    metricFamilies.addAll(metricsReportingScraper.scrape(httpSolrClients, config.getMetricsReporting().getGroup(),config.getMetricsReporting().getType(),config.getMetricsReporting().getPrefix()));
                }

            } catch (SolrServerException | IOException e) {
                logger.error("Get base urls failed: " + e.toString());
            } finally {
                for (HttpSolrClient httpSolrClient : httpSolrClients) {
                    try {
                        httpSolrClient.close();
                    } catch (IOException e) {
                        logger.error("Close client failed: " + e.toString());
                    }
                }
            }
        } else {
            // collect ping status via Ping
            if (config.getPing().getEnable()) {
                PingScraper pingScraper = new PingScraper();
                metricFamilies.addAll(pingScraper.scrape(Collections.singletonList((HttpSolrClient) this.solrClient), config.getPing().getCores()));
            }

            // collect cores status via CoreAdminAPI
            if (config.getCoreAdminAPIStatus().getEnable()) {
                CoreAdminAPIStatusScraper coreAdminStatusCollector = new CoreAdminAPIStatusScraper();
                metricFamilies.addAll(coreAdminStatusCollector.scrape(Collections.singletonList((HttpSolrClient) this.solrClient), config.getCoreAdminAPIStatus().getCores()));
            }

            // collect MBean stats via MBean Query Handler
            if (config.getmBeanRequestHandler().getEnable()) {
                MBeanRequestHandlerScraper mBeanRequestHandlerScraper = new MBeanRequestHandlerScraper();
                metricFamilies.addAll(mBeanRequestHandlerScraper.scrape(Collections.singletonList((HttpSolrClient) this.solrClient), config.getmBeanRequestHandler().getCores(), config.getmBeanRequestHandler().getCat(), config.getmBeanRequestHandler().getKey()));
            }

            // collect metrics via MetricsReporting Reporting
            if (config.getMetricsReporting().getEnable()) {
                MetricsReportingScraper metricsReportingScraper = new MetricsReportingScraper();
                metricFamilies.addAll(metricsReportingScraper.scrape(Collections.singletonList((HttpSolrClient) this.solrClient), config.getMetricsReporting().getGroup(),config.getMetricsReporting().getType(),config.getMetricsReporting().getPrefix()));
            }

        }

        // facets
        if (config.getFacet().getEnable()) {
            FacetScraper facetScraper = new FacetScraper();
            metricFamilies.addAll(facetScraper.collectFacet(this.solrClient, config.getFacet().getQueries()));
        }

        // duration
        List<MetricFamilySamples.Sample> durationSample = new ArrayList<>();
        durationSample.add(new MetricFamilySamples.Sample("solr_scrape_duration_seconds", new ArrayList<>(), new ArrayList<>(), (System.nanoTime() - startTime) / 1.0E9));
        metricFamilies.add(new MetricFamilySamples("solr_scrape_duration_seconds", Type.GAUGE, "Time this Solr scrape took, in seconds.", durationSample));

        return metricFamilies;
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
        String jsonResponse = (String) coreAdminResponse.get("response");

        String jsonPath = "$.status..name";
        JSONArray names = JsonPath.read(jsonResponse, jsonPath);
        for (int i = 0; i < names.size(); i++) {
            String name = (String) names.get(i);
            cores.add(name);
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
        List<String> collections;

        NoOpResponseParser responseParser = new NoOpResponseParser();
        responseParser.setWriterType("json");

        cloudSolrClient.setParser(responseParser);

        CollectionAdminRequest collectionAdminRequest = new CollectionAdminRequest.List();

        NamedList<Object> collectionAdminResponse = cloudSolrClient.request(collectionAdminRequest);
        String jsonResponse = (String) collectionAdminResponse.get("response");

        collections = JsonPath.read(jsonResponse, "$.collections");

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
        String jsonResponse = (String) collectionAdminResponse.get("response");

        String jsonPath = "$.cluster..base_url";
        JSONArray values = JsonPath.read(jsonResponse, jsonPath);
        for (int i = 0; i < values.size(); i++) {
            String baseUrl = (String) values.get(i);
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
