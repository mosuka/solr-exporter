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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mosuka.solr.prometheus.scraper.config.SolrQueryConfig;
import com.github.mosuka.solr.prometheus.scraper.config.SolrScraperConfig;
import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;

/**
 * SolrScraper
 *
 */
public class SolrScraper implements Callable<Map<String, Collector.MetricFamilySamples>> {
    private static final Logger logger = LoggerFactory.getLogger(SolrScraper.class);

    private SolrClient solrClient;
    private SolrScraperConfig scraperConfig;

    /**
     *
     */
    public SolrScraper(SolrClient solrClient, SolrScraperConfig scraperConfig) {
        super();

        this.solrClient = solrClient;
        this.scraperConfig = scraperConfig;
    }

    /**
     *
     * @return
     * @throws Exception
     */
    @Override
    public Map<String, Collector.MetricFamilySamples> call() throws Exception {
        return collectResponse(this.solrClient, this.scraperConfig);
    }

    /**
     * Collect facet count.
     *
     * @param solrClient
     * @param scraperConfig
     * @return
     */
    public Map<String, Collector.MetricFamilySamples> collectResponse(SolrClient solrClient, SolrScraperConfig scraperConfig) {
        Map<String, Collector.MetricFamilySamples> metricFamilySamplesMap = new LinkedHashMap<>();

        try {
            SolrQueryConfig queryConfig = scraperConfig.getQuery();

            // create Solr request parameters
            ModifiableSolrParams params = new ModifiableSolrParams();
            for (Map<String, String> param : queryConfig.getParams()) {
                for (String name : param.keySet()) {
                    Object obj = param.get(name);
                    if (obj instanceof Number) {
                        params.add(name, obj.toString());
                    } else {
                        params.add(name, param.get(name));
                    }
                }
            }

            // create Solr queryConfig request
            QueryRequest queryRequest = new QueryRequest(params);
            queryRequest.setPath(queryConfig.getPath());

            // invoke Solr
            NamedList<Object> queryResponse = queryConfig.getCollection().isEmpty() ? solrClient.request(queryRequest) : solrClient.request(queryRequest, queryConfig.getCollection());

            ObjectMapper om = new ObjectMapper();
            JsonNode metricsJson = om.readTree((String) queryResponse.get("response"));

            for (String jq : scraperConfig.getJsonQueries()) {
                try {
                    JsonQuery q = JsonQuery.compile(jq);
                    List<JsonNode> results = q.apply(metricsJson);
                    for (JsonNode result : results) {
                        String type = result.get("type").textValue();
                        String name = result.get("name").textValue();
                        String help = result.get("help").textValue();
                        Double value = result.get("value").doubleValue();

                        List<String> labelNames = new ArrayList<>();
                        List<String> labelValues = new ArrayList<>();

                        if (solrClient instanceof HttpSolrClient) {
                            labelNames.add("base_url");
                            labelValues.add(((HttpSolrClient) solrClient).getBaseURL());
                        } else {
                            labelNames.add("zk_host");
                            labelValues.add(((CloudSolrClient) solrClient).getZkHost());
                        }

                        if (scraperConfig.getQuery().getCollection() != null && !scraperConfig.getQuery().getCollection().equals("")) {
                            labelNames.add("collection");
                            labelValues.add(scraperConfig.getQuery().getCollection());
                        }

                        for(Iterator<JsonNode> i = result.get("label_names").iterator();i.hasNext();){
                            JsonNode item = i.next();
                            labelNames.add(item.textValue());
                        }
                        for(Iterator<JsonNode> i = result.get("label_values").iterator();i.hasNext();){
                            JsonNode item = i.next();
                            labelValues.add(item.textValue());
                        }

                        if (!metricFamilySamplesMap.containsKey(name)) {
                            if (type.equals("gauge")) {
                                GaugeMetricFamily gauge = new GaugeMetricFamily(
                                        name,
                                        help,
                                        labelNames);
                                metricFamilySamplesMap.put(name, gauge);
                            }
                        }

                        Collector.MetricFamilySamples.Sample sample = new Collector.MetricFamilySamples.Sample(name, labelNames, labelValues, value);

                        if (!metricFamilySamplesMap.get(name).samples.contains(sample)) {
                            metricFamilySamplesMap.get(name).samples.add(sample);
                        }
                    }
                } catch (JsonQueryException e) {
                    logger.error(e.toString() + " " + jq);
                } finally {
                }
            }
        } catch (HttpSolrClient.RemoteSolrException | SolrServerException | IOException e) {
            logger.error(e.toString());
        } catch (Exception e) {
            logger.error(e.toString());
        } finally {
        }

        return metricFamilySamplesMap;
    }
}
