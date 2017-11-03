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
package com.github.mosuka.solr.prometheus.collector.config;

import com.github.mosuka.solr.prometheus.scraper.config.*;

import java.util.ArrayList;
import java.util.List;

/**
 * CollectorConfig
 *
 */
public class CollectorConfig {
    private ScraperConfig ping = new ScraperConfig();
    private ScraperConfig metrics = new ScraperConfig();
    private ScraperConfig collections = new ScraperConfig();
    private List<ScraperConfig> queries = new ArrayList<>();

    public ScraperConfig getPing() {
        return ping;
    }

    public void setPing(ScraperConfig ping) {
        this.ping = ping;
    }

    public ScraperConfig getMetrics() {
        return metrics;
    }

    public void setMetrics(ScraperConfig metrics) {
        this.metrics = metrics;
    }

    public ScraperConfig getCollections() {
        return collections;
    }

    public void setCollections(ScraperConfig collections) {
        this.collections = collections;
    }

    public List<ScraperConfig> getQueries() {
        return queries;
    }

    public void setQueries(List<ScraperConfig> queries) {
        this.queries = queries;
    }
}
