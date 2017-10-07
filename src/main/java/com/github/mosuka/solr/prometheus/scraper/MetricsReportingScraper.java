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
import net.minidev.json.JSONArray;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.NoOpResponseParser;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MetricsReportingScraper {
    private static final Logger logger = LoggerFactory.getLogger(MetricsReportingScraper.class);

    List<Collector.MetricFamilySamples> metricFamilySamplesList = new ArrayList<>();
    Map<String, Collector.MetricFamilySamples> metricFamilySampleMap = new LinkedHashMap<>();

    /**
     * Collect MetricsReporting Reporting.
     *
     * @param httpSolrClients
     * @return
     */
    public List<Collector.MetricFamilySamples> scrape(List<HttpSolrClient> httpSolrClients, List<String> groups, List<String> types, List<String> prefixes) {
        metricFamilySamplesList = new ArrayList<>();
        metricFamilySampleMap = new LinkedHashMap<>();

        List<Map<String, Object>> responses = new ArrayList<>();

        ModifiableSolrParams params = new ModifiableSolrParams();
        params.set("compact", "true");

        if (groups.size() > 0) {
            params.set("group", String.join(",", groups));
        }

        if (types.size() > 0) {
            params.set("type", String.join(",", types));
        }

        if (prefixes.size() > 0) {
            params.set("prefix", String.join(",", prefixes));
        }

        QueryRequest metricsRequest = new QueryRequest(params);
        metricsRequest.setPath("/admin/metrics");

        for (HttpSolrClient httpSolrClient : httpSolrClients) {
            NoOpResponseParser responseParser = new NoOpResponseParser();
            responseParser.setWriterType("json");

            httpSolrClient.setParser(responseParser);

            try {
                NamedList<Object> metricsResponse = httpSolrClient.request(metricsRequest);

                Map<String, Object> response = new LinkedHashMap<>();

                response.put("baseUrl", httpSolrClient.getBaseURL());
                response.put("response", JsonPath.read((String) metricsResponse.get("response"), "$"));

                // for Solr 6.5.x
                if (((Map<String, Object>) response.get("response")).get("metrics") instanceof JSONArray) {
                    Map<String, Object> newSolrMBeans = new LinkedHashMap<>();
                    for (int i = 0; i < ((JSONArray) ((Map<String, Object>) response.get("response")).get("metrics")).size(); i++) {
                        newSolrMBeans.put(
                                (String) ((JSONArray) ((Map<String, Object>) response.get("response")).get("metrics")).get(i),
                                ((JSONArray) ((Map<String, Object>) response.get("response")).get("metrics")).get(++i));
                    }
                    if (newSolrMBeans.size() > 0) {
                        ((Map<String, Object>) response.get("response")).put("metrics", newSolrMBeans);
                    }
                }

                responses.add(response);
            } catch (Exception e) {
                logger.error("Get metrics response failed: " + e.toString());
            }
        }

        String metricNamePrefix = "solr";

        Pattern pattern;
        Matcher matcher;

        try {
            for (Map<String, Object> response : responses) {
                String baseUrl = (String) response.get("baseUrl");

                Map<String, Object> flattenResponse = SolrCollector.flatten((Map<String, Object>) ((Map<String, Object>) response.get("response")).get("metrics"), "|");
                for (String flattenKey : flattenResponse.keySet()) {
                    Object rawValue = flattenResponse.get(flattenKey);

                    logger.debug("baseUrl: " + baseUrl + ", key=" + flattenKey + ", value=" + rawValue.toString());

                    if (rawValue instanceof String) {
                        logger.debug("key=" + flattenKey + ", value=" + rawValue + ", non Number object");
                        continue;
                    }

                    pattern = Pattern.compile("^solr\\.jvm\\|buffers\\.(?<type>[^\\|\\.]+)\\.(?<item>[^\\|\\.]+)$");
                    matcher = pattern.matcher(flattenKey);
                    if (matcher.matches()) {
                        String mType = "";
                        String mItem = "";

                        try {
                            mType = matcher.group("type") != null ? matcher.group("type") : "";
                            mItem = matcher.group("item") != null ? matcher.group("item") : "";
                        } catch (IllegalArgumentException e) {
                            logger.warn("Scrape failed: " + e.toString());
                        }

                        if (mItem.equals("Count")) {
                            String name = SolrCollector.safeName(String.join("_", metricNamePrefix, "solr.jvm", "buffers", "total"));
                            String help = "See following URL: https://lucene.apache.org/solr/guide/6_6/metrics-reporting.html#jvm-registry-code-solr-jvm-code";
                            List<String> labelName = Arrays.asList("baseUrl", "type");
                            List<String> labelValue = Arrays.asList(baseUrl, mType);
                            Double value = ((Number) rawValue).doubleValue();

                            addMetricFamilySample(name, help, labelName, labelValue, value);
                        } else {
                            String name = SolrCollector.safeName(String.join("_", metricNamePrefix, "solr.jvm", "buffers", "bytes"));
                            String help = "See following URL: https://lucene.apache.org/solr/guide/6_6/metrics-reporting.html#jvm-registry-code-solr-jvm-code";
                            List<String> labelName = Arrays.asList("baseUrl", "type", "item");
                            List<String> labelValue = Arrays.asList(baseUrl, mType, mItem.equals("MemoryUsed") ? "used" : mItem.equals("TotalCapacity") ? "total" : "");
                            Double value = ((Number) rawValue).doubleValue();

                            addMetricFamilySample(name, help, labelName, labelValue, value);
                        }

                        continue;
                    }

                    pattern = Pattern.compile("^solr\\.jvm\\|classes\\.(?<item>[^.]+)$");
                    matcher = pattern.matcher(flattenKey);
                    if (matcher.matches()) {
                        String mItem = "";

                        try {
                            mItem = matcher.group("item") != null ? matcher.group("item") : "";
                        } catch (IllegalArgumentException e) {
                            logger.warn("Scrape failed: " + e.toString());
                        }

                        String name = SolrCollector.safeName(String.join("_", metricNamePrefix, "solr.jvm", "classes", "total"));
                        String help = "See following URL: https://lucene.apache.org/solr/guide/6_6/metrics-reporting.html#jvm-registry-code-solr-jvm-code";
                        List<String> labelName = Arrays.asList("baseUrl", "item");
                        List<String> labelValue = Arrays.asList(baseUrl, mItem);
                        Double value = ((Number) rawValue).doubleValue();

                        addMetricFamilySample(name, help, labelName, labelValue, value);

                        continue;
                    }

                    pattern = Pattern.compile("^solr\\.jvm\\|gc\\.(?<type>[^.]+)\\.(?<item>[^.]+)$");
                    matcher = pattern.matcher(flattenKey);
                    if (matcher.matches()) {
                        String mType = "";
                        String mItem = "";

                        try {
                            mType = matcher.group("type") != null ? matcher.group("type") : "";
                            mItem = matcher.group("item") != null ? matcher.group("item") : "";
                        } catch (IllegalArgumentException e) {
                            logger.warn("Scrape failed: " + e.toString());
                        }

                        if (mItem.equals("count")) {
                            String name = SolrCollector.safeName(String.join("_", metricNamePrefix, "solr.jvm", "gc", mItem, "total"));
                            String help = "See following URL: https://lucene.apache.org/solr/guide/6_6/metrics-reporting.html#jvm-registry-code-solr-jvm-code";
                            List<String> labelName = Arrays.asList("baseUrl", "type");
                            List<String> labelValue = Arrays.asList(baseUrl, mType);
                            Double value = ((Number) rawValue).doubleValue();

                            addMetricFamilySample(name, help, labelName, labelValue, value);
                        } else if (mItem.equals("time")) {
                            String name = SolrCollector.safeName(String.join("_", metricNamePrefix, "solr.jvm", "gc", mItem, "seconds"));
                            String help = "See following URL: https://lucene.apache.org/solr/guide/6_6/metrics-reporting.html#jvm-registry-code-solr-jvm-code";
                            List<String> labelName = Arrays.asList("baseUrl", "type");
                            List<String> labelValue = Arrays.asList(baseUrl, mType);
                            Double value = ((Number) rawValue).doubleValue() / 1000.0;

                            addMetricFamilySample(name, help, labelName, labelValue, value);
                        } else {
                            logger.warn("key=" + flattenKey + ", value=" + rawValue + " does not match patterns");
                        }

                        continue;
                    }

                    pattern = Pattern.compile("^solr\\.jvm\\|memory\\.(?<type>[^.]+)(|\\.(?<space>[^.]+))\\.(?<item>[^.]+)$");
                    matcher = pattern.matcher(flattenKey);
                    if (matcher.matches()) {
                        String mType = "";
                        String mSpace = "";
                        String mItem = "";

                        try {
                            mType = matcher.group("type") != null ? matcher.group("type") : "";
                            mSpace = matcher.group("space") != null ? matcher.group("space") : "";
                            mItem = matcher.group("item") != null ? matcher.group("item") : "";
                        } catch (IllegalArgumentException e) {
                            logger.warn("Scrape failed: " + e.toString());
                        }

                        if (mItem.equals("usage")) {
                            String name = SolrCollector.safeName(String.join("_", metricNamePrefix, "solr.jvm", "memory", mItem, "ratio"));
                            String help = "See following URL: https://lucene.apache.org/solr/guide/6_6/metrics-reporting.html#jvm-registry-code-solr-jvm-code";
                            List<String> labelName = Arrays.asList("baseUrl", "type", "space");
                            List<String> labelValue = Arrays.asList(baseUrl, mType, mSpace);
                            Double value = ((Number) rawValue).doubleValue();

                            addMetricFamilySample(name, help, labelName, labelValue, value);
                        } else {
                            String name = SolrCollector.safeName(String.join("_", metricNamePrefix, "solr.jvm", "memory", "bytes"));
                            String help = "See following URL: https://lucene.apache.org/solr/guide/6_6/metrics-reporting.html#jvm-registry-code-solr-jvm-code";
                            List<String> labelName = Arrays.asList("baseUrl", "type", "space", "item");
                            List<String> labelValue = Arrays.asList(baseUrl, mType, mSpace, mItem);
                            Double value = ((Number) rawValue).doubleValue();

                            addMetricFamilySample(name, help, labelName, labelValue, value);
                        }

                        continue;
                    }

                    pattern = Pattern.compile("^solr\\.jvm\\|os\\.(?<item>[^.]+)$");
                    matcher = pattern.matcher(flattenKey);
                    if (matcher.matches()) {
                        String mItem = "";

                        try {
                            mItem = matcher.group("item") != null ? matcher.group("item") : "";
                        } catch (IllegalArgumentException e) {
                            logger.warn("Scrape failed: " + e.toString());
                        }

                        if (mItem.equals("availableProcessors")) {
                            String name = SolrCollector.safeName(String.join("_", metricNamePrefix, "solr.jvm", "os", "processors", "total"));
                            String help = "See following URL: https://lucene.apache.org/solr/guide/6_6/metrics-reporting.html#jvm-registry-code-solr-jvm-code";
                            List<String> labelName = Arrays.asList("baseUrl", "item");
                            List<String> labelValue = Arrays.asList(baseUrl, "available");
                            Double value = ((Number) rawValue).doubleValue();

                            addMetricFamilySample(name, help, labelName, labelValue, value);
                        } else if (mItem.equals("freePhysicalMemorySize") || mItem.equals("freeSwapSpaceSize") || mItem.equals("totalPhysicalMemorySize") || mItem.equals("totalSwapSpaceSize") || mItem.equals("committedVirtualMemorySize")) {
                            String name = SolrCollector.safeName(String.join("_", metricNamePrefix, "solr.jvm", "os", "memory", "bytes"));
                            String help = "See following URL: https://lucene.apache.org/solr/guide/6_6/metrics-reporting.html#jvm-registry-code-solr-jvm-code";

                            List<String> labelNames = Arrays.asList("baseUrl", "item");
                            List<String> labelValues = Arrays.asList(baseUrl, mItem);
                            Double value = ((Number) rawValue).doubleValue();

                            addMetricFamilySample(name, help, labelNames, labelValues, value);
                        } else if (mItem.equals("maxFileDescriptorCount") || mItem.equals("openFileDescriptorCount")) {
                            String name = SolrCollector.safeName(String.join("_", metricNamePrefix, "solr.jvm", "os", "fileDescriptor", "total"));
                            String help = "See following URL: https://lucene.apache.org/solr/guide/6_6/metrics-reporting.html#jvm-registry-code-solr-jvm-code";
                            List<String> labelName = Arrays.asList("baseUrl", "item");
                            List<String> labelValue = Arrays.asList(baseUrl, mItem);
                            Double value = ((Number) rawValue).doubleValue();

                            addMetricFamilySample(name, help, labelName, labelValue, value);
                        } else if (mItem.equals("processCpuLoad") || mItem.equals("systemCpuLoad")) {
                            String name = SolrCollector.safeName(String.join("_", metricNamePrefix, "solr.jvm", "os", "cpuLoad", "ratio"));
                            String help = "See following URL: https://lucene.apache.org/solr/guide/6_6/metrics-reporting.html#jvm-registry-code-solr-jvm-code";
                            List<String> labelName = Arrays.asList("baseUrl", "item");
                            List<String> labelValue = Arrays.asList(baseUrl, mItem);
                            Double value = ((Number) rawValue).doubleValue();

                            addMetricFamilySample(name, help, labelName, labelValue, value);
                        } else if (mItem.equals("processCpuTime")) {
                            String name = SolrCollector.safeName(String.join("_", metricNamePrefix, "solr.jvm", "os", "cpuTime", "seconds"));
                            String help = "See following URL: https://lucene.apache.org/solr/guide/6_6/metrics-reporting.html#jvm-registry-code-solr-jvm-code";
                            List<String> labelName = Arrays.asList("baseUrl", "type");
                            List<String> labelValue = Arrays.asList(baseUrl, "process");
                            Double value = ((Number) rawValue).doubleValue() / 1000.0;

                            addMetricFamilySample(name, help, labelName, labelValue, value);
                        } else if (mItem.equals("systemLoadAverage")) {
                            String name = SolrCollector.safeName(String.join("_", metricNamePrefix, "solr.jvm", "os", "loadAverage"));
                            String help = "See following URL: https://lucene.apache.org/solr/guide/6_6/metrics-reporting.html#jvm-registry-code-solr-jvm-code";
                            List<String> labelName = Arrays.asList("baseUrl", "item");
                            List<String> labelValue = Arrays.asList(baseUrl, mItem);
                            Double value = ((Number) rawValue).doubleValue();

                            addMetricFamilySample(name, help, labelName, labelValue, value);
                        } else {
                            logger.warn("key=" + flattenKey + ", value=" + rawValue + " does not match patterns");
                        }

                        continue;
                    }

                    pattern = Pattern.compile("^solr\\.jvm\\|threads(?:|\\.(?<type>[^.]+))\\.count$");
                    matcher = pattern.matcher(flattenKey);
                    if (matcher.matches()) {
                        String mType = "";

                        try {
                            mType = matcher.group("type") != null ? matcher.group("type") : "";
                        } catch (IllegalArgumentException e) {
                            logger.warn("Scrape failed: " + e.toString());
                        }

                        String name = SolrCollector.safeName(String.join("_", metricNamePrefix, "solr.jvm", "threads", "total"));
                        String help = "See following URL: https://lucene.apache.org/solr/guide/6_6/metrics-reporting.html#jvm-registry-code-solr-jvm-code";
                        List<String> labelName = Arrays.asList("baseUrl", "type");
                        List<String> labelValue = Arrays.asList(baseUrl, mType);
                        Double value = ((Number) rawValue).doubleValue();

                        addMetricFamilySample(name, help, labelName, labelValue, value);

                        continue;
                    }

                    pattern = Pattern.compile("^solr\\.node\\|(?<category>[^|\\.]+)\\.(?<handler>[^|\\.]+)\\.clientErrors\\|(?<item>[^|\\.]+)$");
                    matcher = pattern.matcher(flattenKey);
                    if (matcher.matches()) {
                        String mCategory = "";
                        String mHandler = "";
                        String mItem = "";

                        try {
                            mCategory = matcher.group("category") != null ? matcher.group("category") : "";
                            mHandler = matcher.group("handler") != null ? matcher.group("handler") : "";
                            mItem = matcher.group("item") != null ? matcher.group("item") : "";
                        } catch (IllegalArgumentException e) {
                            logger.warn("Scrape failed: " + e.toString());
                        }

                        String name = SolrCollector.safeName(String.join("_", metricNamePrefix, "node", "clientErrors", "total"));
                        String help = "See following URL: https://lucene.apache.org/solr/guide/6_6/metrics-reporting.html#node-corecontainer-registry-code-solr-node-code";
                        List<String> labelName = Arrays.asList("baseUrl", "category", "handler", "item");
                        List<String> labelValue = Arrays.asList(baseUrl, mCategory, mHandler, mItem);
                        Double value = ((Number) rawValue).doubleValue();

                        addMetricFamilySample(name, help, labelName, labelValue, value);

                        continue;
                    }

                    pattern = Pattern.compile("^solr\\.node\\|(?<category>[^|\\.]+)\\.(?<handler>[^|\\.]+)\\.errors\\|(?<item>[^|\\.]+)$");
                    matcher = pattern.matcher(flattenKey);
                    if (matcher.matches()) {
                        String mCategory = "";
                        String mHandler = "";
                        String mItem = "";

                        try {
                            mCategory = matcher.group("category") != null ? matcher.group("category") : "";
                            mHandler = matcher.group("handler") != null ? matcher.group("handler") : "";
                            mItem = matcher.group("item") != null ? matcher.group("item") : "";
                        } catch (IllegalArgumentException e) {
                            logger.warn("Scrape failed: " + e.toString());
                        }

                        String name = SolrCollector.safeName(String.join("_", metricNamePrefix, "node", "errors", "total"));
                        String help = "See following URL: https://lucene.apache.org/solr/guide/6_6/metrics-reporting.html#node-corecontainer-registry-code-solr-node-code";
                        List<String> labelName = Arrays.asList("baseUrl", "category", "handler", "item");
                        List<String> labelValue = Arrays.asList(baseUrl, mCategory, mHandler, mItem);
                        Double value = ((Number) rawValue).doubleValue();

                        addMetricFamilySample(name, help, labelName, labelValue, value);

                        continue;
                    }

                    pattern = Pattern.compile("^solr\\.node\\|(?<category>[^|\\.]+)\\.(?<handler>[^|\\.]+)\\.requestTimes\\|(?<item>[^|\\.]+)$");
                    matcher = pattern.matcher(flattenKey);
                    if (matcher.matches()) {
                        String mCategory = "";
                        String mHandler = "";
                        String mItem = "";

                        try {
                            mCategory = matcher.group("category") != null ? matcher.group("category") : "";
                            mHandler = matcher.group("handler") != null ? matcher.group("handler") : "";
                            mItem = matcher.group("item") != null ? matcher.group("item") : "";
                        } catch (IllegalArgumentException e) {
                            logger.warn("Scrape failed: " + e.toString());
                        }

                        if (mItem.equals("count") || mItem.equals("meanRate") || mItem.equals("1minRate") || mItem.equals("5minRate") || mItem.equals("15minRate")) {
                            String name = SolrCollector.safeName(String.join("_", metricNamePrefix, "node", "requests", "total"));
                            String help = "See following URL: https://lucene.apache.org/solr/guide/6_6/metrics-reporting.html#node-corecontainer-registry-code-solr-node-code";
                            List<String> labelName = Arrays.asList("baseUrl", "category", "handler", "item");
                            List<String> labelValue = Arrays.asList(baseUrl, mCategory, mHandler, mItem);
                            Double value = ((Number) rawValue).doubleValue();

                            addMetricFamilySample(name, help, labelName, labelValue, value);
                        } else if (mItem.equals("min_ms") || mItem.equals("max_ms") || mItem.equals("mean_ms") || mItem.equals("median_ms") || mItem.equals("stddev_ms") || mItem.equals("p75_ms") || mItem.equals("p95_ms") || mItem.equals("p99_ms") || mItem.equals("p999_ms")) {
                            String name = SolrCollector.safeName(String.join("_", metricNamePrefix, "node", "requests", "seconds"));
                            String help = "See following URL: https://lucene.apache.org/solr/guide/6_6/metrics-reporting.html#node-corecontainer-registry-code-solr-node-code";
                            List<String> labelName = Arrays.asList("baseUrl", "category", "handler", "item");
                            List<String> labelValue = Arrays.asList(baseUrl, mCategory, mHandler, mItem.replaceFirst("_ms", ""));
                            Double value = ((Number) rawValue).doubleValue() / 1000.0;

                            addMetricFamilySample(name, help, labelName, labelValue, value);
                        } else {
                            logger.warn("key=" + flattenKey + ", value=" + rawValue + " does not match patterns");
                        }

                        continue;
                    }

                    pattern = Pattern.compile("^solr\\.node\\|(?<category>[^|\\.]+)\\.(?<handler>[^|\\.]+)\\.requests$");
                    matcher = pattern.matcher(flattenKey);
                    if (matcher.matches()) {
                        String mCategory = "";
                        String mHandler = "";

                        try {
                            mCategory = matcher.group("category") != null ? matcher.group("category") : "";
                            mHandler = matcher.group("handler") != null ? matcher.group("handler") : "";
                        } catch (IllegalArgumentException e) {
                            logger.warn("Scrape failed: " + e.toString());
                        }

                        String name = SolrCollector.safeName(String.join("_", metricNamePrefix, "node", "requests", "total"));
                        String help = "See following URL: https://lucene.apache.org/solr/guide/6_6/metrics-reporting.html#node-corecontainer-registry-code-solr-node-code";
                        List<String> labelName = Arrays.asList("baseUrl", "category", "handler", "item");
                        List<String> labelValue = Arrays.asList(baseUrl, mCategory, mHandler, "count");
                        Double value = ((Number) rawValue).doubleValue();

                        addMetricFamilySample(name, help, labelName, labelValue, value);

                        continue;
                    }

                    pattern = Pattern.compile("^solr\\.node\\|(?<category>[^|\\.]+)\\.(?<handler>[^|\\.]+)\\.serverErrors\\|(?<item>[^|\\.]+)$");
                    matcher = pattern.matcher(flattenKey);
                    if (matcher.matches()) {
                        String mCategory = "";
                        String mHandler = "";
                        String mItem = "";

                        try {
                            mCategory = matcher.group("category") != null ? matcher.group("category") : "";
                            mHandler = matcher.group("handler") != null ? matcher.group("handler") : "";
                            mItem = matcher.group("item") != null ? matcher.group("item") : "";
                        } catch (IllegalArgumentException e) {
                            logger.warn("Scrape failed: " + e.toString());
                        }

                        String name = SolrCollector.safeName(String.join("_", metricNamePrefix, "node", "serverErrors", "total"));
                        String help = "See following URL: https://lucene.apache.org/solr/guide/6_6/metrics-reporting.html#node-corecontainer-registry-code-solr-node-code";
                        List<String> labelName = Arrays.asList("baseUrl", "category", "handler", "item");
                        List<String> labelValue = Arrays.asList(baseUrl, mCategory, mHandler, mItem);
                        Double value = ((Number) rawValue).doubleValue();

                        addMetricFamilySample(name, help, labelName, labelValue, value);

                        continue;
                    }

                    pattern = Pattern.compile("^solr\\.node\\|(?<category>[^|\\.]+)\\.(?<handler>[^|\\.]+)\\.timeouts\\|(?<item>[^|\\.]+)$");
                    matcher = pattern.matcher(flattenKey);
                    if (matcher.matches()) {
                        String mCategory = "";
                        String mHandler = "";
                        String mItem = "";

                        try {
                            mCategory = matcher.group("category") != null ? matcher.group("category") : "";
                            mHandler = matcher.group("handler") != null ? matcher.group("handler") : "";
                            mItem = matcher.group("item") != null ? matcher.group("item") : "";
                        } catch (IllegalArgumentException e) {
                            logger.warn("Scrape failed: " + e.toString());
                        }

                        String name = SolrCollector.safeName(String.join("_", metricNamePrefix, "node", "timeouts", "total"));
                        String help = "See following URL: https://lucene.apache.org/solr/guide/6_6/metrics-reporting.html#node-corecontainer-registry-code-solr-node-code";
                        List<String> labelName = Arrays.asList("baseUrl", "category", "handler", "item");
                        List<String> labelValue = Arrays.asList(baseUrl, mCategory, mHandler, mItem);
                        Double value = ((Number) rawValue).doubleValue();

                        addMetricFamilySample(name, help, labelName, labelValue, value);

                        continue;
                    }

                    pattern = Pattern.compile("^solr\\.node\\|(?<category>[^|\\.]+)\\.(?<handler>[^|\\.]+)\\.totalTime$");
                    matcher = pattern.matcher(flattenKey);
                    if (matcher.matches()) {
                        String mCategory = "";
                        String mHandler = "";

                        try {
                            mCategory = matcher.group("category") != null ? matcher.group("category") : "";
                            mHandler = matcher.group("handler") != null ? matcher.group("handler") : "";
                        } catch (IllegalArgumentException e) {
                            logger.warn("Scrape failed: " + e.toString());
                        }

                        String name = SolrCollector.safeName(String.join("_", metricNamePrefix, "node", "totalTime", "seconds"));
                        String help = "See following URL: https://lucene.apache.org/solr/guide/6_6/metrics-reporting.html#node-corecontainer-registry-code-solr-node-code";
                        List<String> labelName = Arrays.asList("baseUrl", "category", "handler");
                        List<String> labelValue = Arrays.asList(baseUrl, mCategory, mHandler);
                        Double value = ((Number) rawValue).doubleValue() / 1000.0;

                        addMetricFamilySample(name, help, labelName, labelValue, value);

                        continue;
                    }

                    pattern = Pattern.compile("^solr\\.node\\|(?<category>[^|\\.]+)\\.(?<handler>[^|\\.]+)\\.(?<endpoint>https?:\\/\\/[\\w:\\/\\.]+)\\.(?<method>[^\\|\\.]+)\\.requests\\|(?<item>[^|\\.]+)$");
                    matcher = pattern.matcher(flattenKey);
                    if (matcher.matches()) {
                        String mCategory = "";
                        String mHandler = "";
                        String mEndpoint = "";
                        String mMethod = "";
                        String mItem = "";

                        try {
                            mCategory = matcher.group("category") != null ? matcher.group("category") : "";
                            mHandler = matcher.group("handler") != null ? matcher.group("handler") : "";
                            mEndpoint = matcher.group("endpoint") != null ? matcher.group("endpoint") : "";
                            mMethod = matcher.group("method") != null ? matcher.group("method") : "";
                            mItem = matcher.group("item") != null ? matcher.group("item") : "";
                        } catch (IllegalArgumentException e) {
                            logger.warn("Scrape failed: " + e.toString());
                        }

                        if (mItem.equals("count") || mItem.equals("meanRate") || mItem.equals("1minRate") || mItem.equals("5minRate") || mItem.equals("15minRate")) {
                            String name = SolrCollector.safeName(String.join("_", metricNamePrefix, "node", "requests", "total"));
                            String help = "See following URL: https://lucene.apache.org/solr/guide/6_6/metrics-reporting.html#node-corecontainer-registry-code-solr-node-code";
                            List<String> labelName = Arrays.asList("baseUrl", "category", "handler", "endpoint", "method", "item");
                            List<String> labelValue = Arrays.asList(baseUrl, mCategory, mHandler, mEndpoint, mMethod, mItem);
                            Double value = ((Number) rawValue).doubleValue();

                            addMetricFamilySample(name, help, labelName, labelValue, value);
                        } else if (mItem.equals("min_ms") || mItem.equals("max_ms") || mItem.equals("mean_ms") || mItem.equals("median_ms") || mItem.equals("stddev_ms") || mItem.equals("p75_ms") || mItem.equals("p95_ms") || mItem.equals("p99_ms") || mItem.equals("p999_ms")) {
                            String name = SolrCollector.safeName(String.join("_", metricNamePrefix, "node", "requests", "seconds"));
                            String help = "See following URL: https://lucene.apache.org/solr/guide/6_6/metrics-reporting.html#node-corecontainer-registry-code-solr-node-code";
                            List<String> labelName = Arrays.asList("baseUrl", "category", "handler", "endpoint", "method", "item");
                            List<String> labelValue = Arrays.asList(baseUrl, mCategory, mHandler, mEndpoint, mMethod, mItem.replaceFirst("_ms", ""));
                            Double value = ((Number) rawValue).doubleValue() / 1000.0;

                            addMetricFamilySample(name, help, labelName, labelValue, value);
                        } else {
                            logger.warn("key=" + flattenKey + ", value=" + rawValue + " does not match patterns");
                        }

                        continue;
                    }

                    pattern = Pattern.compile("^solr\\.node\\|(?<category>[^|\\.]+)(|\\.(?<handler>[^|\\.]+))\\.threadPool\\.(?<executor>[^|\\.]+)\\.(?<type>completed|submitted|duration)\\|(?<item>[^|\\.]+)$");
                    matcher = pattern.matcher(flattenKey);
                    if (matcher.matches()) {
                        String mCategory = "";
                        String mHandler = "";
                        String mExecutor = "";
                        String mType = "";
                        String mItem = "";

                        try {
                            mCategory = matcher.group("category") != null ? matcher.group("category") : "";
                            mHandler = matcher.group("handler") != null ? matcher.group("handler") : "";
                            mExecutor = matcher.group("executor") != null ? matcher.group("executor") : "";
                            mType = matcher.group("type") != null ? matcher.group("type") : "";
                            mItem = matcher.group("item") != null ? matcher.group("item") : "";
                        } catch (IllegalArgumentException e) {
                            logger.warn("Scrape failed: " + e.toString());
                        }

                        if (mItem.equals("count") || mItem.equals("meanRate") || mItem.equals("1minRate") || mItem.equals("5minRate") || mItem.equals("15minRate")) {
                            String name = SolrCollector.safeName(String.join("_", metricNamePrefix, "node", "threadPool", mType, "total"));
                            String help = "See following URL: https://lucene.apache.org/solr/guide/6_6/metrics-reporting.html#node-corecontainer-registry-code-solr-node-code";
                            List<String> labelName = Arrays.asList("baseUrl", "category", "handler", "executor", "item");
                            List<String> labelValue = Arrays.asList(baseUrl, mCategory, mHandler, mExecutor, mItem);
                            Double value = ((Number) rawValue).doubleValue();

                            addMetricFamilySample(name, help, labelName, labelValue, value);
                        } else if (mItem.equals("min_ms") || mItem.equals("max_ms") || mItem.equals("mean_ms") || mItem.equals("median_ms") || mItem.equals("stddev_ms") || mItem.equals("p75_ms") || mItem.equals("p95_ms") || mItem.equals("p99_ms") || mItem.equals("p999_ms")) {
                            String name = SolrCollector.safeName(String.join("_", metricNamePrefix, "node", "threadPool", mType, "seconds"));
                            String help = "See following URL: https://lucene.apache.org/solr/guide/6_6/metrics-reporting.html#node-corecontainer-registry-code-solr-node-code";
                            List<String> labelName = Arrays.asList("baseUrl", "category", "handler", "executor", "item");
                            List<String> labelValue = Arrays.asList(baseUrl, mCategory, mHandler, mExecutor, mItem.replaceFirst("_ms", ""));
                            Double value = ((Number) rawValue).doubleValue() / 1000.0;

                            addMetricFamilySample(name, help, labelName, labelValue, value);
                        } else {
                            logger.warn("key=" + flattenKey + ", value=" + rawValue + " does not match patterns");
                        }

                        continue;
                    }

                    pattern = Pattern.compile("^solr\\.node\\|(?<category>[^|\\.]+)(|\\.(?<handler>[^|\\.]+))\\.threadPool\\.(?<executor>[^|\\.]+)\\.running$");
                    matcher = pattern.matcher(flattenKey);
                    if (matcher.matches()) {
                        String mCategory = "";
                        String mHandler = "";
                        String mExecutor = "";

                        try {
                            mCategory = matcher.group("category") != null ? matcher.group("category") : "";
                            mHandler = matcher.group("handler") != null ? matcher.group("handler") : "";
                            mExecutor = matcher.group("executor") != null ? matcher.group("executor") : "";
                        } catch (IllegalArgumentException e) {
                            logger.warn("Scrape failed: " + e.toString());
                        }

                        String name = SolrCollector.safeName(String.join("_", metricNamePrefix, "node", "threadPool", "runnning", "total"));
                        String help = "See following URL: https://lucene.apache.org/solr/guide/6_6/metrics-reporting.html#node-corecontainer-registry-code-solr-node-code";
                        List<String> labelName = Arrays.asList("baseUrl", "category", "handler", "executor");
                        List<String> labelValue = Arrays.asList(baseUrl, mCategory, mHandler, mExecutor);
                        Double value = ((Number) rawValue).doubleValue();

                        addMetricFamilySample(name, help, labelName, labelValue, value);

                        continue;
                    }

                    pattern = Pattern.compile("^solr\\.node\\|(?<category>CACHE)\\.fieldCache\\|(?<item>[^|\\.]+)$");
                    matcher = pattern.matcher(flattenKey);
                    if (matcher.matches()) {
                        String mCategory = "";
                        String mItem = "";

                        try {
                            mCategory = matcher.group("category") != null ? matcher.group("category") : "";
                            mItem = matcher.group("item") != null ? matcher.group("item") : "";
                        } catch (IllegalArgumentException e) {
                            logger.warn("Scrape failed: " + e.toString());
                        }

                        String name = SolrCollector.safeName(String.join("_", metricNamePrefix, "node", "fieldCache", "total"));
                        String help = "See following URL: https://lucene.apache.org/solr/guide/6_6/metrics-reporting.html#node-corecontainer-registry-code-solr-node-code";
                        List<String> labelName = Arrays.asList("baseUrl", "category", "item");
                        List<String> labelValue = Arrays.asList(baseUrl, mCategory, mItem);
                        Double value = ((Number) rawValue).doubleValue();

                        addMetricFamilySample(name, help, labelName, labelValue, value);

                        continue;
                    }

                    pattern = Pattern.compile("^solr\\.node\\|(?<category>[^|\\.]+)\\.cores\\.(?<item>[^|\\.]+)$");
                    matcher = pattern.matcher(flattenKey);
                    if (matcher.matches()) {
                        String mCategory = "";
                        String mItem = "";

                        try {
                            mCategory = matcher.group("category") != null ? matcher.group("category") : "";
                            mItem = matcher.group("item") != null ? matcher.group("item") : "";
                        } catch (IllegalArgumentException e) {
                            logger.warn("Scrape failed: " + e.toString());
                        }

                        String name = SolrCollector.safeName(String.join("_", metricNamePrefix, "node", "cores", "total"));
                        String help = "See following URL: https://lucene.apache.org/solr/guide/6_6/metrics-reporting.html#node-corecontainer-registry-code-solr-node-code";
                        List<String> labelName = Arrays.asList("baseUrl", "category", "item");
                        List<String> labelValue = Arrays.asList(baseUrl, mCategory, mItem);
                        Double value = ((Number) rawValue).doubleValue();

                        addMetricFamilySample(name, help, labelName, labelValue, value);

                        continue;
                    }

                    pattern = Pattern.compile("^solr\\.node\\|(?<category>[^|\\.]+)\\.fs\\.(?<item>[^|\\.]+)$");
                    matcher = pattern.matcher(flattenKey);
                    if (matcher.matches()) {
                        String mCategory = "";
                        String mItem = "";

                        try {
                            mCategory = matcher.group("category") != null ? matcher.group("category") : "";
                            mItem = matcher.group("item") != null ? matcher.group("item") : "";
                        } catch (IllegalArgumentException e) {
                            logger.warn("Scrape failed: " + e.toString());
                        }

                        String name = SolrCollector.safeName(String.join("_", metricNamePrefix, "node", "fs", "bytes"));
                        String help = "See following URL: https://lucene.apache.org/solr/guide/6_6/metrics-reporting.html#node-corecontainer-registry-code-solr-node-code";
                        List<String> labelName = Arrays.asList("baseUrl", "category", "item");
                        List<String> labelValue = Arrays.asList(baseUrl, mCategory, mItem);
                        Double value = ((Number) rawValue).doubleValue();

                        addMetricFamilySample(name, help, labelName, labelValue, value);

                        continue;
                    }

                    pattern = Pattern.compile("^solr\\.node\\|(?<category>[^|\\.]+)\\.(?<handler>[^|\\.]+)\\.(?<item>[^|\\.]+Connections)$");
                    matcher = pattern.matcher(flattenKey);
                    if (matcher.matches()) {
                        String mCategory = "";
                        String mHandler = "";
                        String mItem = "";

                        try {
                            mCategory = matcher.group("category") != null ? matcher.group("category") : "";
                            mHandler = matcher.group("handler") != null ? matcher.group("handler") : "";
                            mItem = matcher.group("item") != null ? matcher.group("item") : "";
                        } catch (IllegalArgumentException e) {
                            logger.warn("Scrape failed: " + e.toString());
                        }

                        String name = SolrCollector.safeName(String.join("_", metricNamePrefix, "node", "connections", "total"));
                        String help = "See following URL: https://lucene.apache.org/solr/guide/6_6/metrics-reporting.html#node-corecontainer-registry-code-solr-node-code";
                        List<String> labelName = Arrays.asList("baseUrl", "category", "handler", "item");
                        List<String> labelValue = Arrays.asList(baseUrl, mCategory, mHandler, mItem);
                        Double value = ((Number) rawValue).doubleValue();

                        addMetricFamilySample(name, help, labelName, labelValue, value);

                        continue;
                    }

                    pattern = Pattern.compile("^solr\\.core\\.(?<collection>[^|\\.]+)\\.(?<shard>[^|\\.]+)\\.(?<replica>[^|\\.]+)\\|(?<category>[^|\\.]+)\\.(?<handler>[^|\\.]+)\\.requests$");
                    matcher = pattern.matcher(flattenKey);
                    if (matcher.matches()) {
                        String mCollection = "";
                        String mShard = "";
                        String mReplica = "";
                        String mCategory = "";
                        String mHandler = "";

                        try {
                            mCollection = matcher.group("collection") != null ? matcher.group("collection") : "";
                            mShard = matcher.group("shard") != null ? matcher.group("shard") : "";
                            mReplica = matcher.group("replica") != null ? matcher.group("replica") : "";
                            mCategory = matcher.group("category") != null ? matcher.group("category") : "";
                            mHandler = matcher.group("handler") != null ? matcher.group("handler") : "";
                        } catch (IllegalArgumentException e) {
                            logger.warn("Scrape failed: " + e.toString());
                        }

                        String name = SolrCollector.safeName(String.join("_", metricNamePrefix, "core", "requests", "total"));
                        String help = "See following URL: https://lucene.apache.org/solr/guide/6_6/metrics-reporting.html#core-solrcore-registry";
                        List<String> labelName = Arrays.asList("baseUrl", "collection", "shard", "replica", "category", "handler");
                        List<String> labelValue = Arrays.asList(baseUrl, mCollection, mShard, mReplica, mCategory, mHandler);
                        Double value = ((Number) rawValue).doubleValue();

                        addMetricFamilySample(name, help, labelName, labelValue, value);

                        continue;
                    }

                    pattern = Pattern.compile("^solr\\.core\\.(?<collection>[^|\\.]+)\\.(?<shard>[^|\\.]+)\\.(?<replica>[^|\\.]+)\\|(?<category>[^|\\.]+)\\.(?<handler>[^|\\.]+)\\.totalTime$");
                    matcher = pattern.matcher(flattenKey);
                    if (matcher.matches()) {
                        String mCollection = "";
                        String mShard = "";
                        String mReplica = "";
                        String mCategory = "";
                        String mHandler = "";

                        try {
                            mCollection = matcher.group("collection") != null ? matcher.group("collection") : "";
                            mShard = matcher.group("shard") != null ? matcher.group("shard") : "";
                            mReplica = matcher.group("replica") != null ? matcher.group("replica") : "";
                            mCategory = matcher.group("category") != null ? matcher.group("category") : "";
                            mHandler = matcher.group("handler") != null ? matcher.group("handler") : "";
                        } catch (IllegalArgumentException e) {
                            logger.warn("Scrape failed: " + e.toString());
                        }

                        String name = SolrCollector.safeName(String.join("_", metricNamePrefix, "core", "totalTime", "seconds"));
                        String help = "See following URL: https://lucene.apache.org/solr/guide/6_6/metrics-reporting.html#core-solrcore-registry";
                        List<String> labelName = Arrays.asList("baseUrl", "collection", "shard", "replica", "category", "handler");
                        List<String> labelValue = Arrays.asList(baseUrl, mCollection, mShard, mReplica, mCategory, mHandler);
                        Double value = ((Number) rawValue).doubleValue() / 1000.0;

                        addMetricFamilySample(name, help, labelName, labelValue, value);

                        continue;
                    }

                    pattern = Pattern.compile("^solr\\.core\\.(?<collection>[^|\\.]+)\\.(?<shard>[^|\\.]+)\\.(?<replica>[^|\\.]+)\\|REPLICATION\\.(?<type>errors|skipped)$");
                    matcher = pattern.matcher(flattenKey);
                    if (matcher.matches()) {
                        String mCollection = "";
                        String mShard = "";
                        String mReplica = "";
                        String mType = "";

                        try {
                            mCollection = matcher.group("collection") != null ? matcher.group("collection") : "";
                            mShard = matcher.group("shard") != null ? matcher.group("shard") : "";
                            mReplica = matcher.group("replica") != null ? matcher.group("replica") : "";
                            mType = matcher.group("type") != null ? matcher.group("type") : "";
                        } catch (IllegalArgumentException e) {
                            logger.warn("Scrape failed: " + e.toString());
                        }

                        String name = SolrCollector.safeName(String.join("_", metricNamePrefix, "core", "replication", "status", "total"));
                        String help = "See following URL: https://lucene.apache.org/solr/guide/6_6/metrics-reporting.html#core-solrcore-registry";
                        List<String> labelName = Arrays.asList("baseUrl", "collection", "shard", "replica", "type");
                        List<String> labelValue = Arrays.asList(baseUrl, mCollection, mShard, mReplica, mType);
                        Double value = ((Number) rawValue).doubleValue();

                        addMetricFamilySample(name, help, labelName, labelValue, value);

                        continue;
                    }

                    pattern = Pattern.compile("^solr\\.core\\.(?<collection>[^|\\.]+)\\.(?<shard>[^|\\.]+)\\.(?<replica>[^|\\.]+)\\|SEARCHER\\.new(|\\.(?<item>[^|\\.]+))$");
                    matcher = pattern.matcher(flattenKey);
                    if (matcher.matches()) {
                        String mCollection = "";
                        String mShard = "";
                        String mReplica = "";
                        String mItem = "";

                        try {
                            mCollection = matcher.group("collection") != null ? matcher.group("collection") : "";
                            mShard = matcher.group("shard") != null ? matcher.group("shard") : "";
                            mReplica = matcher.group("replica") != null ? matcher.group("replica") : "";
                            mItem = matcher.group("item") != null ? matcher.group("item") : "";
                        } catch (IllegalArgumentException e) {
                            logger.warn("Scrape failed: " + e.toString());
                        }

                        String name = SolrCollector.safeName(String.join("_", metricNamePrefix, "core", "searcher", "new", "status", "total"));
                        String help = "See following URL: https://lucene.apache.org/solr/guide/6_6/metrics-reporting.html#core-solrcore-registry";
                        List<String> labelName = Arrays.asList("baseUrl", "collection", "shard", "replica", "item");
                        List<String> labelValue = Arrays.asList(baseUrl, mCollection, mShard, mReplica, mItem.equals("") ? "count" : mItem);
                        Double value = ((Number) rawValue).doubleValue();

                        addMetricFamilySample(name, help, labelName, labelValue, value);

                        continue;
                    }

                    pattern = Pattern.compile("^solr\\.core\\.(?<collection>[^|\\.]+)\\.(?<shard>[^|\\.]+)\\.(?<replica>[^|\\.]+)\\|CACHE\\.core\\.(?<type>[^|\\.]+)\\|(?<item>[^|\\.]+)$");
                    matcher = pattern.matcher(flattenKey);
                    if (matcher.matches()) {
                        String mCollection = "";
                        String mShard = "";
                        String mReplica = "";
                        String mType = "";
                        String mItem = "";

                        try {
                            mCollection = matcher.group("collection") != null ? matcher.group("collection") : "";
                            mShard = matcher.group("shard") != null ? matcher.group("shard") : "";
                            mReplica = matcher.group("replica") != null ? matcher.group("replica") : "";
                            mType = matcher.group("type") != null ? matcher.group("type") : "";
                            mItem = matcher.group("item") != null ? matcher.group("item") : "";
                        } catch (IllegalArgumentException e) {
                            logger.warn("Scrape failed: " + e.toString());
                        }

                        String name = SolrCollector.safeName(String.join("_", metricNamePrefix, "core", "cache", "core", mItem, "total"));
                        String help = "See following URL: https://lucene.apache.org/solr/guide/6_6/metrics-reporting.html#core-solrcore-registry";
                        List<String> labelName = Arrays.asList("baseUrl", "collection", "shard", "replica", "type");
                        List<String> labelValue = Arrays.asList(baseUrl, mCollection, mShard, mReplica, mType);
                        Double value = ((Number) rawValue).doubleValue();

                        addMetricFamilySample(name, help, labelName, labelValue, value);

                        continue;
                    }

                    pattern = Pattern.compile("^solr\\.core\\.(?<collection>[^|\\.]+)\\.(?<shard>[^|\\.]+)\\.(?<replica>[^|\\.]+)\\|CACHE\\.searcher\\.(?<type>[^|\\.]+)\\|(?<item>[^|\\.]+)$");
                    matcher = pattern.matcher(flattenKey);
                    if (matcher.matches()) {
                        String mCollection = "";
                        String mShard = "";
                        String mReplica = "";
                        String mType = "";
                        String mItem = "";

                        try {
                            mCollection = matcher.group("collection") != null ? matcher.group("collection") : "";
                            mShard = matcher.group("shard") != null ? matcher.group("shard") : "";
                            mReplica = matcher.group("replica") != null ? matcher.group("replica") : "";
                            mType = matcher.group("type") != null ? matcher.group("type") : "";
                            mItem = matcher.group("item") != null ? matcher.group("item") : "";
                        } catch (IllegalArgumentException e) {
                            logger.warn("Scrape failed: " + e.toString());
                        }

                        if (mItem.equals("warmupTime")) {
                            String name = SolrCollector.safeName(String.join("_", metricNamePrefix, "core", "cache", "searcher", mItem, "seconds"));
                            String help = "See following URL: https://lucene.apache.org/solr/guide/6_6/metrics-reporting.html#core-solrcore-registry";
                            List<String> labelName = Arrays.asList("baseUrl", "collection", "shard", "replica", "type");
                            List<String> labelValue = Arrays.asList(baseUrl, mCollection, mShard, mReplica, mType);
                            Double value = ((Number) rawValue).doubleValue() / 1000.0;

                            addMetricFamilySample(name, help, labelName, labelValue, value);
                        } else if (mItem.endsWith("ratio")) {
                            String name = SolrCollector.safeName(String.join("_", metricNamePrefix, "core", "cache", "searcher", mItem.replace("ratio", ""), "ratio"));
                            String help = "See following URL: https://lucene.apache.org/solr/guide/6_6/metrics-reporting.html#core-solrcore-registry";
                            List<String> labelName = Arrays.asList("baseUrl", "collection", "shard", "replica", "type");
                            List<String> labelValue = Arrays.asList(baseUrl, mCollection, mShard, mReplica, mType);
                            Double value = ((Number) rawValue).doubleValue();

                            addMetricFamilySample(name, help, labelName, labelValue, value);
                        } else {
                            String name = SolrCollector.safeName(String.join("_", metricNamePrefix, "core", "cache", "searcher", mItem, "total"));
                            String help = "See following URL: https://lucene.apache.org/solr/guide/6_6/metrics-reporting.html#core-solrcore-registry";
                            List<String> labelName = Arrays.asList("baseUrl", "collection", "shard", "replica", "type");
                            List<String> labelValue = Arrays.asList(baseUrl, mCollection, mShard, mReplica, mType);
                            Double value = ((Number) rawValue).doubleValue();

                            addMetricFamilySample(name, help, labelName, labelValue, value);
                        }

                        continue;
                    }

                    pattern = Pattern.compile("^solr\\.core\\.(?<collection>[^|\\.]+)\\.(?<shard>[^|\\.]+)\\.(?<replica>[^|\\.]+)\\|CORE\\.fs\\.(?<item>[^|\\.]+)$");
                    matcher = pattern.matcher(flattenKey);
                    if (matcher.matches()) {
                        String mCollection = "";
                        String mShard = "";
                        String mReplica = "";
                        String mItem = "";

                        try {
                            mCollection = matcher.group("collection") != null ? matcher.group("collection") : "";
                            mShard = matcher.group("shard") != null ? matcher.group("shard") : "";
                            mReplica = matcher.group("replica") != null ? matcher.group("replica") : "";
                            mItem = matcher.group("item") != null ? matcher.group("item") : "";
                        } catch (IllegalArgumentException e) {
                            logger.warn("Scrape failed: " + e.toString());
                        }

                        String name = SolrCollector.safeName(String.join("_", metricNamePrefix, "core", "fs", "bytes"));
                        String help = "See following URL: https://lucene.apache.org/solr/guide/6_6/metrics-reporting.html#core-solrcore-registry";
                        List<String> labelName = Arrays.asList("baseUrl", "collection", "shard", "replica", "item");
                        List<String> labelValue = Arrays.asList(baseUrl, mCollection, mShard, mReplica, mItem);
                        Double value = ((Number) rawValue).doubleValue();

                        addMetricFamilySample(name, help, labelName, labelValue, value);

                        continue;
                    }

                    pattern = Pattern.compile("^solr\\.core\\.(?<collection>[^|\\.]+)\\.(?<shard>[^|\\.]+)\\.(?<replica>[^|\\.]+)\\|CORE\\.refCount$");
                    matcher = pattern.matcher(flattenKey);
                    if (matcher.matches()) {
                        String mCollection = "";
                        String mShard = "";
                        String mReplica = "";

                        try {
                            mCollection = matcher.group("collection") != null ? matcher.group("collection") : "";
                            mShard = matcher.group("shard") != null ? matcher.group("shard") : "";
                            mReplica = matcher.group("replica") != null ? matcher.group("replica") : "";
                        } catch (IllegalArgumentException e) {
                            logger.warn("Scrape failed: " + e.toString());
                        }

                        String name = SolrCollector.safeName(String.join("_", metricNamePrefix, "core", "refCount", "total"));
                        String help = "See following URL: https://lucene.apache.org/solr/guide/6_6/metrics-reporting.html#core-solrcore-registry";
                        List<String> labelName = Arrays.asList("baseUrl", "collection", "shard", "replica");
                        List<String> labelValue = Arrays.asList(baseUrl, mCollection, mShard, mReplica);
                        Double value = ((Number) rawValue).doubleValue();

                        addMetricFamilySample(name, help, labelName, labelValue, value);

                        continue;
                    }

                    pattern = Pattern.compile("^solr\\.core\\.(?<collection>[^|\\.]+)\\.(?<shard>[^|\\.]+)\\.(?<replica>[^|\\.]+)\\|INDEX\\.sizeInBytes$");
                    matcher = pattern.matcher(flattenKey);
                    if (matcher.matches()) {
                        String mCollection = "";
                        String mShard = "";
                        String mReplica = "";

                        try {
                            mCollection = matcher.group("collection") != null ? matcher.group("collection") : "";
                            mShard = matcher.group("shard") != null ? matcher.group("shard") : "";
                            mReplica = matcher.group("replica") != null ? matcher.group("replica") : "";
                        } catch (IllegalArgumentException e) {
                            logger.warn("Scrape failed: " + e.toString());
                        }

                        String name = SolrCollector.safeName(String.join("_", metricNamePrefix, "core", "index", "size", "bytes"));
                        String help = "See following URL: https://lucene.apache.org/solr/guide/6_6/metrics-reporting.html#core-solrcore-registry";
                        List<String> labelName = Arrays.asList("baseUrl", "collection", "shard", "replica");
                        List<String> labelValue = Arrays.asList(baseUrl, mCollection, mShard, mReplica);
                        Double value = ((Number) rawValue).doubleValue();

                        addMetricFamilySample(name, help, labelName, labelValue, value);

                        continue;
                    }

                    pattern = Pattern.compile("^solr\\.core\\.(?<collection>[^|\\.]+)\\.(?<shard>[^|\\.]+)\\.(?<replica>[^|\\.]+)\\|SEARCHER\\.searcher\\|(?<item>numDocs|deletedDocs|maxDoc)$");
                    matcher = pattern.matcher(flattenKey);
                    if (matcher.matches()) {
                        String mCollection = "";
                        String mShard = "";
                        String mReplica = "";
                        String mItem = "";

                        try {
                            mCollection = matcher.group("collection") != null ? matcher.group("collection") : "";
                            mShard = matcher.group("shard") != null ? matcher.group("shard") : "";
                            mReplica = matcher.group("replica") != null ? matcher.group("replica") : "";
                            mItem = matcher.group("item") != null ? matcher.group("item") : "";
                        } catch (IllegalArgumentException e) {
                            logger.warn("Scrape failed: " + e.toString());
                        }

                        String name = SolrCollector.safeName(String.join("_", metricNamePrefix, "core", "searcher", "documents", "total"));
                        String help = "See following URL: https://lucene.apache.org/solr/guide/6_6/metrics-reporting.html#core-solrcore-registry";
                        List<String> labelName = Arrays.asList("baseUrl", "collection", "shard", "replica", "item");
                        List<String> labelValue = Arrays.asList(baseUrl, mCollection, mShard, mReplica, mItem);
                        Double value = ((Number) rawValue).doubleValue();

                        addMetricFamilySample(name, help, labelName, labelValue, value);

                        continue;
                    }

                    pattern = Pattern.compile("^solr\\.core\\.(?<collection>[^|\\.]+)\\.(?<shard>[^|\\.]+)\\.(?<replica>[^|\\.]+)\\|SEARCHER\\.searcher\\|indexVersion$");
                    matcher = pattern.matcher(flattenKey);
                    if (matcher.matches()) {
                        String mCollection = "";
                        String mShard = "";
                        String mReplica = "";

                        try {
                            mCollection = matcher.group("collection") != null ? matcher.group("collection") : "";
                            mShard = matcher.group("shard") != null ? matcher.group("shard") : "";
                            mReplica = matcher.group("replica") != null ? matcher.group("replica") : "";
                        } catch (IllegalArgumentException e) {
                            logger.warn("Scrape failed: " + e.toString());
                        }

                        String name = SolrCollector.safeName(String.join("_", metricNamePrefix, "core", "searcher", "indexVersion"));
                        String help = "See following URL: https://lucene.apache.org/solr/guide/6_6/metrics-reporting.html#core-solrcore-registry";
                        List<String> labelName = Arrays.asList("baseUrl", "collection", "shard", "replica");
                        List<String> labelValue = Arrays.asList(baseUrl, mCollection, mShard, mReplica);
                        Double value = ((Number) rawValue).doubleValue();

                        addMetricFamilySample(name, help, labelName, labelValue, value);

                        continue;
                    }

                    pattern = Pattern.compile("^solr\\.core\\.(?<collection>[^|\\.]+)\\.(?<shard>[^|\\.]+)\\.(?<replica>[^|\\.]+)\\|SEARCHER\\.searcher\\|warmupTime$");
                    matcher = pattern.matcher(flattenKey);
                    if (matcher.matches()) {
                        String mCollection = "";
                        String mShard = "";
                        String mReplica = "";

                        try {
                            mCollection = matcher.group("collection") != null ? matcher.group("collection") : "";
                            mShard = matcher.group("shard") != null ? matcher.group("shard") : "";
                            mReplica = matcher.group("replica") != null ? matcher.group("replica") : "";
                        } catch (IllegalArgumentException e) {
                            logger.warn("Scrape failed: " + e.toString());
                        }

                        String name = SolrCollector.safeName(String.join("_", metricNamePrefix, "core", "searcher", "warmupTime", "seconds"));
                        String help = "See following URL: https://lucene.apache.org/solr/guide/6_6/metrics-reporting.html#core-solrcore-registry";
                        List<String> labelName = Arrays.asList("baseUrl", "collection", "shard", "replica");
                        List<String> labelValue = Arrays.asList(baseUrl, mCollection, mShard, mReplica);
                        Double value = ((Number) rawValue).doubleValue() / 1000.0;

                        addMetricFamilySample(name, help, labelName, labelValue, value);

                        continue;
                    }

                    pattern = Pattern.compile("^solr\\.core\\.(?<collection>[^|\\.]+)\\.(?<shard>[^|\\.]+)\\.(?<replica>[^|\\.]+)\\|TLOG\\.buffered\\.ops$");
                    matcher = pattern.matcher(flattenKey);
                    if (matcher.matches()) {
                        String mCollection = "";
                        String mShard = "";
                        String mReplica = "";

                        try {
                            mCollection = matcher.group("collection") != null ? matcher.group("collection") : "";
                            mShard = matcher.group("shard") != null ? matcher.group("shard") : "";
                            mReplica = matcher.group("replica") != null ? matcher.group("replica") : "";
                        } catch (IllegalArgumentException e) {
                            logger.warn("Scrape failed: " + e.toString());
                        }

                        String name = SolrCollector.safeName(String.join("_", metricNamePrefix, "core", "tlog", "buffered", "ops", "total"));
                        String help = "See following URL: https://lucene.apache.org/solr/guide/6_6/metrics-reporting.html#core-solrcore-registry";
                        List<String> labelName = Arrays.asList("baseUrl", "collection", "shard", "replica");
                        List<String> labelValue = Arrays.asList(baseUrl, mCollection, mShard, mReplica);
                        Double value = ((Number) rawValue).doubleValue();

                        addMetricFamilySample(name, help, labelName, labelValue, value);

                        continue;
                    }

                    pattern = Pattern.compile("^solr\\.core\\.(?<collection>[^|\\.]+)\\.(?<shard>[^|\\.]+)\\.(?<replica>[^|\\.]+)\\|TLOG\\.replay\\.remaining\\.bytes$");
                    matcher = pattern.matcher(flattenKey);
                    if (matcher.matches()) {
                        String mCollection = "";
                        String mShard = "";
                        String mReplica = "";

                        try {
                            mCollection = matcher.group("collection") != null ? matcher.group("collection") : "";
                            mShard = matcher.group("shard") != null ? matcher.group("shard") : "";
                            mReplica = matcher.group("replica") != null ? matcher.group("replica") : "";
                        } catch (IllegalArgumentException e) {
                            logger.warn("Scrape failed: " + e.toString());
                        }

                        String name = SolrCollector.safeName(String.join("_", metricNamePrefix, "core", "tlog", "replay", "remaining", "bytes"));
                        String help = "See following URL: https://lucene.apache.org/solr/guide/6_6/metrics-reporting.html#core-solrcore-registry";
                        List<String> labelName = Arrays.asList("baseUrl", "collection", "shard", "replica");
                        List<String> labelValue = Arrays.asList(baseUrl, mCollection, mShard, mReplica);
                        Double value = ((Number) rawValue).doubleValue();

                        addMetricFamilySample(name, help, labelName, labelValue, value);

                        continue;
                    }

                    pattern = Pattern.compile("^solr\\.core\\.(?<collection>[^|\\.]+)\\.(?<shard>[^|\\.]+)\\.(?<replica>[^|\\.]+)\\|TLOG\\.replay\\.remaining\\.logs$");
                    matcher = pattern.matcher(flattenKey);
                    if (matcher.matches()) {
                        String mCollection = "";
                        String mShard = "";
                        String mReplica = "";

                        try {
                            mCollection = matcher.group("collection") != null ? matcher.group("collection") : "";
                            mShard = matcher.group("shard") != null ? matcher.group("shard") : "";
                            mReplica = matcher.group("replica") != null ? matcher.group("replica") : "";
                        } catch (IllegalArgumentException e) {
                            logger.warn("Scrape failed: " + e.toString());
                        }

                        String name = SolrCollector.safeName(String.join("_", metricNamePrefix, "core", "tlog", "replay", "remaining", "logs", "total"));
                        String help = "See following URL: https://lucene.apache.org/solr/guide/6_6/metrics-reporting.html#core-solrcore-registry";
                        List<String> labelName = Arrays.asList("baseUrl", "collection", "shard", "replica");
                        List<String> labelValue = Arrays.asList(baseUrl, mCollection, mShard, mReplica);
                        Double value = ((Number) rawValue).doubleValue();

                        addMetricFamilySample(name, help, labelName, labelValue, value);

                        continue;
                    }

                    pattern = Pattern.compile("^solr\\.core\\.(?<collection>[^|\\.]+)\\.(?<shard>[^|\\.]+)\\.(?<replica>[^|\\.]+)\\|TLOG\\.state$");
                    matcher = pattern.matcher(flattenKey);
                    if (matcher.matches()) {
                        String mCollection = "";
                        String mShard = "";
                        String mReplica = "";

                        try {
                            mCollection = matcher.group("collection") != null ? matcher.group("collection") : "";
                            mShard = matcher.group("shard") != null ? matcher.group("shard") : "";
                            mReplica = matcher.group("replica") != null ? matcher.group("replica") : "";
                        } catch (IllegalArgumentException e) {
                            logger.warn("Scrape failed: " + e.toString());
                        }

                        String name = SolrCollector.safeName(String.join("_", metricNamePrefix, "core", "tlog", "state"));
                        String help = "See following URL: https://lucene.apache.org/solr/guide/6_6/metrics-reporting.html#core-solrcore-registry";
                        List<String> labelName = Arrays.asList("baseUrl", "collection", "shard", "replica");
                        List<String> labelValue = Arrays.asList(baseUrl, mCollection, mShard, mReplica);
                        Double value = ((Number) rawValue).doubleValue();

                        addMetricFamilySample(name, help, labelName, labelValue, value);

                        continue;
                    }











                    logger.warn("key=" + flattenKey + ", value=" + rawValue + " does not match patterns");
                }
            }
        } catch (Exception e){
            logger.error("Collect failed: " + e.toString());
        }

        for (String gaugeMetricName : metricFamilySampleMap.keySet()) {
            if (metricFamilySampleMap.get(gaugeMetricName).samples.size() > 0) {
                metricFamilySamplesList.add(metricFamilySampleMap.get(gaugeMetricName));
            }
        }

        return metricFamilySamplesList;
    }

    private void addMetricFamilySample(String name, String help, List<String> labelNames, List<String> labelValues, Double value) {
        Collector.MetricFamilySamples metricFamilySamples;

        if (!metricFamilySampleMap.containsKey(name)) {
            metricFamilySamples = new GaugeMetricFamily(
                    name,
                    help,
                    labelNames);
            metricFamilySampleMap.put(name, metricFamilySamples);

//            logger.debug("Add MetricFamilySamples: " + metricFamilySamples.toString());
        }

        Collector.MetricFamilySamples.Sample sample = new Collector.MetricFamilySamples.Sample(name, labelNames, labelValues,value);
//        logger.debug("Add MetricFamilySamples Sample: " + sample.toString());

        if (!metricFamilySampleMap.get(name).samples.contains(sample)) {
            metricFamilySampleMap.get(name).samples.add(sample);
        }

        return;
    }
}
