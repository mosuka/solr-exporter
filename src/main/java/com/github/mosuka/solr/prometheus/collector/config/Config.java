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

public class Config {
//    private String baseUrl = "";

//    private List<String> zkHosts = new ArrayList<>();
//    private String znode = "";

    private List<ScraperConfig> scraperConfigs = new ArrayList<>();

//    public String getBaseUrl() {
//        return baseUrl;
//    }

//    public void setBaseUrl(String baseUrl) {
//        this.baseUrl = baseUrl;
//    }

//    public List<String> getZkHosts() {
//        return zkHosts;
//    }

//    public void setZkHosts(List<String> zkHosts) {
//        this.zkHosts = zkHosts;
//    }

//    public String getZnode() {
//        return znode;
//    }

//    public void setZnode(String zkNode) {
//        this.znode = zkNode;
//    }

    public List<ScraperConfig> getScraperConfigs() {
        return scraperConfigs;
    }

    public void setScraperConfigs(List<ScraperConfig> scraperConfigs) {
        this.scraperConfigs = scraperConfigs;
    }
}
