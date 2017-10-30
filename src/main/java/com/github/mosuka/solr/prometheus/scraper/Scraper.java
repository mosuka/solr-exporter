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
import com.github.mosuka.solr.prometheus.scraper.config.Query;
import com.github.mosuka.solr.prometheus.scraper.config.ScraperConfig;
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

public class Scraper {
    private static final Logger logger = LoggerFactory.getLogger(Scraper.class);

    /**
     * Collect facet count.
     *
     * @param solrClient
     * @param scraperConfigs
     * @return
     */
    public List<Collector.MetricFamilySamples> collectResponse(SolrClient solrClient, List<ScraperConfig> scraperConfigs) {
        List<Collector.MetricFamilySamples> metricFamilySamplesList = new ArrayList<>();
        Map<String, Collector.MetricFamilySamples> metricFamilySampleMap = new LinkedHashMap<>();


        for (ScraperConfig scraperConfig : scraperConfigs) {
            try {
                Query query = scraperConfig.getQuery();

                // create Solr request parameters
                ModifiableSolrParams params = new ModifiableSolrParams();
                for (Map<String, String> param : query.getParams()) {
                    for (String name : param.keySet()) {
                        Object obj = param.get(name);
                        if (obj instanceof Number) {
                            params.add(name, obj.toString());
                        } else {
                            params.add(name, param.get(name));
                        }
                    }
                }

                // create Solr query request
                QueryRequest queryRequest = new QueryRequest(params);
                queryRequest.setPath(query.getPath());

                // invoke Solr
                NamedList<Object> queryResponse = query.getCollection().isEmpty() ? solrClient.request(queryRequest) : solrClient.request(queryRequest, query.getCollection());

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

                            for(Iterator<JsonNode> i = result.get("label_names").iterator();i.hasNext();){
                                JsonNode item = i.next();
                                labelNames.add(item.textValue());
                            }
                            for(Iterator<JsonNode> i = result.get("label_values").iterator();i.hasNext();){
                                JsonNode item = i.next();
                                labelValues.add(item.textValue());
                            }

                            if (!metricFamilySampleMap.containsKey(name)) {
                                if (type.equals("gauge")) {
                                    GaugeMetricFamily gauge = new GaugeMetricFamily(
                                            name,
                                            help,
                                            labelNames);
                                    metricFamilySampleMap.put(name, gauge);
                                }
                            }

                            Collector.MetricFamilySamples.Sample sample = new Collector.MetricFamilySamples.Sample(name, labelNames, labelValues, value);
                            if (!metricFamilySampleMap.get(name).samples.contains(sample)) {
                                metricFamilySampleMap.get(name).samples.add(sample);
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
        }

        for (String gaugeMetricName : metricFamilySampleMap.keySet()) {
            if (metricFamilySampleMap.get(gaugeMetricName).samples.size() > 0) {
                metricFamilySamplesList.add(metricFamilySampleMap.get(gaugeMetricName));
            }
        }

        return metricFamilySamplesList;
    }
}
