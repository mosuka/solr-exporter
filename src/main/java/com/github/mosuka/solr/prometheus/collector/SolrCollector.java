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
import com.github.mosuka.solr.prometheus.collector.config.SolrCollectorConfig;
import com.github.mosuka.solr.prometheus.scraper.*;
import com.github.mosuka.solr.prometheus.scraper.config.SolrScraperConfig;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * SolrCollector
 *
 */
public class SolrCollector extends Collector implements Collector.Describable {
    private static final Logger logger = LoggerFactory.getLogger(SolrCollector.class);

    private SolrClient solrClient;
    private SolrCollectorConfig config = new SolrCollectorConfig();
    private int numThreads;

    private static ObjectMapper om = new ObjectMapper();

    /**
     * Constructor.
     *
     * @param solrClient
     * @param config
     */
    public SolrCollector(SolrClient solrClient, SolrCollectorConfig config, int numThreads) {
        this.solrClient = solrClient;
        this.config = config;
        this.numThreads = numThreads;
    }

    /**
     * Describe scrape status.
     *
     * @return
     */
    public List<Collector.MetricFamilySamples> describe() {
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

        List<HttpSolrClient> httpSolrClients = new ArrayList<>();
        if (solrClient instanceof CloudSolrClient) {
            try {
                httpSolrClients = getHttpSolrClients((CloudSolrClient) solrClient);
            } catch (SolrServerException | IOException e) {
                logger.error(e.getMessage());
            }
        }

        Map<String, MetricFamilySamples> metricFamilySamplesMap = new LinkedHashMap<>();

        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
        List<Future<Map<String, MetricFamilySamples>>> futureList = new ArrayList<>();

        try {
            // Ping
            if (config.getPing() != null) {
                if (solrClient instanceof CloudSolrClient) {
                    for (HttpSolrClient httpSolrClient : httpSolrClients) {
                        if (config.getPing().getQuery().getCollection().equals("")) {
                            try {
                                for (String core : getCores(httpSolrClient)) {
                                    try {
                                        SolrScraperConfig pingConfig = config.getPing().clone();
                                        pingConfig.getQuery().setCollection(core);

                                        SolrScraper scraper = new SolrScraper(httpSolrClient, pingConfig);
                                        Future<Map<String, MetricFamilySamples>> future = executorService.submit(scraper);
                                        futureList.add(future);
                                    } catch (CloneNotSupportedException e) {
                                        logger.error(e.getMessage());
                                    }
                                }
                            } catch (SolrServerException | IOException e) {
                                logger.error(e.getMessage());
                            }
                        } else {
                            SolrScraper scraper = new SolrScraper(httpSolrClient, config.getPing());
                            Future<Map<String, MetricFamilySamples>> future = executorService.submit(scraper);
                            futureList.add(future);
                        }
                    }
                } else {
                    if (config.getPing().getQuery().getCollection().equals("")) {
                        try {
                            for (String core : getCores((HttpSolrClient) solrClient)) {
                                try {
                                    SolrScraperConfig pingConfig = config.getPing().clone();
                                    pingConfig.getQuery().setCollection(core);

                                    SolrScraper scraper = new SolrScraper(solrClient, pingConfig);
                                    Future<Map<String, MetricFamilySamples>> future = executorService.submit(scraper);
                                    futureList.add(future);
                                } catch (CloneNotSupportedException e) {
                                    logger.error(e.getMessage());
                                }
                            }
                        } catch (SolrServerException | IOException e) {
                            logger.error(e.getMessage());
                        }
                    } else {
                        SolrScraper scraper = new SolrScraper(solrClient, config.getPing());
                        Future<Map<String, MetricFamilySamples>> future = executorService.submit(scraper);
                        futureList.add(future);
                    }
                }
            }

            // Metrics
            if (config.getMetrics() != null) {
                if (solrClient instanceof CloudSolrClient) {
                    for (HttpSolrClient httpSolrClient : httpSolrClients) {
                        SolrScraper scraper = new SolrScraper(httpSolrClient, config.getMetrics());
                        Future<Map<String, MetricFamilySamples>> future = executorService.submit(scraper);
                        futureList.add(future);
                    }
                } else {
                    SolrScraper scraper = new SolrScraper(solrClient, config.getMetrics());
                    Future<Map<String, MetricFamilySamples>> future = executorService.submit(scraper);
                    futureList.add(future);
                }
            }

            // Collections
            if (config.getCollections() != null) {
                if (solrClient instanceof CloudSolrClient) {
                    SolrScraper scraper = new SolrScraper(solrClient, config.getCollections());
                    Future<Map<String, MetricFamilySamples>> future = executorService.submit(scraper);
                    futureList.add(future);
                }
            }

            // Query
            if (config.getQueries() != null) {
                for (SolrScraperConfig c : config.getQueries()) {
                    SolrScraper scraper = new SolrScraper(solrClient, c);
                    Future<Map<String, MetricFamilySamples>> future = executorService.submit(scraper);
                    futureList.add(future);
                }
            }

            // get future
            for (Future<Map<String, MetricFamilySamples>> future : futureList) {
                try {
                    mergeMetrics(metricFamilySamplesMap, future.get());
                } catch (InterruptedException | ExecutionException e) {
                    logger.error(e.getMessage());
                }
            }
        } finally {
            for (HttpSolrClient httpSolrClient : httpSolrClients) {
                try {
                    httpSolrClient.close();
                } catch (IOException e) {
                    logger.error(e.getMessage());
                }
            }
        }

        // return value
        List<MetricFamilySamples> metricFamiliesSamplesList = new ArrayList<>();

        // add solr metrics
        for (String gaugeMetricName : metricFamilySamplesMap.keySet()) {
            MetricFamilySamples metricFamilySamples = metricFamilySamplesMap.get(gaugeMetricName);

            if (metricFamilySamples.samples.size() > 0) {
                metricFamiliesSamplesList.add(metricFamilySamples);
            }
        }

        // add scrape duration metric
        List<MetricFamilySamples.Sample> durationSample = new ArrayList<>();
        durationSample.add(new MetricFamilySamples.Sample("solr_scrape_duration_seconds", new ArrayList<>(), new ArrayList<>(), (System.nanoTime() - startTime) / 1.0E9));
        metricFamiliesSamplesList.add(new MetricFamilySamples("solr_scrape_duration_seconds", Type.GAUGE, "Time this Solr scrape took, in seconds.", durationSample));

        return metricFamiliesSamplesList;
    }

    /**
     *
     * @param metrics1
     * @param metrics2
     * @return
     */
    private Map<String, MetricFamilySamples> mergeMetrics(Map<String, MetricFamilySamples> metrics1, Map<String, MetricFamilySamples> metrics2) {

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
}
