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

public class CollectorConfig {
    private ScraperConfig pingConfig = new ScraperConfig();
    private ScraperConfig metricsConfig = new ScraperConfig();
    private ScraperConfig collectionsConfig = new ScraperConfig();
    private List<ScraperConfig> queryConfigs = new ArrayList<>();

    public ScraperConfig getPingConfig() {
        return pingConfig;
    }

    public void setPingConfig(ScraperConfig pingConfig) {
        this.pingConfig = pingConfig;
    }

    public ScraperConfig getMetricsConfig() {
        return metricsConfig;
    }

    public void setMetricsConfig(ScraperConfig metricsConfig) {
        this.metricsConfig = metricsConfig;
    }

    public ScraperConfig getCollectionsConfig() {
        return collectionsConfig;
    }

    public void setCollectionsConfig(ScraperConfig collectionsConfig) {
        this.collectionsConfig = collectionsConfig;
    }

    public List<ScraperConfig> getQueryConfigs() {
        return queryConfigs;
    }

    public void setQueryConfigs(List<ScraperConfig> queryConfigs) {
        this.queryConfigs = queryConfigs;
    }
}
