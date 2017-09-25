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
    private String baseUrl = "";

    private List<String> zkHosts = new ArrayList<>();
    private String znode = "";

    private Ping ping = new Ping();

    private CoreAdminAPIStatus coreAdminAPIStatus = new CoreAdminAPIStatus();

    private MBeanRequestHandler mBeanRequestHandler = new MBeanRequestHandler();

    private CollectionsAPIOverseerStatus collectionsAPIOverseerStatus = new CollectionsAPIOverseerStatus();

    private CollectionsAPIClusterStatus collectionsAPIClusterStatus = new CollectionsAPIClusterStatus();

    private MetricsReporting metricsReporting = new MetricsReporting();

    private Facet facet = new Facet();

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public List<String> getZkHosts() {
        return zkHosts;
    }

    public void setZkHosts(List<String> zkHosts) {
        this.zkHosts = zkHosts;
    }

    public String getZnode() {
        return znode;
    }

    public void setZnode(String zkNode) {
        this.znode = zkNode;
    }

    public Ping getPing() {
        return ping;
    }

    public void setPing(Ping ping) {
        this.ping = ping;
    }

    public CoreAdminAPIStatus getCoreAdminAPIStatus() {
        return coreAdminAPIStatus;
    }

    public void setCoreAdminAPIStatus(CoreAdminAPIStatus coreAdminAPIStatus) {
        this.coreAdminAPIStatus = coreAdminAPIStatus;
    }

    public MBeanRequestHandler getmBeanRequestHandler() {
        return mBeanRequestHandler;
    }

    public void setmBeanRequestHandler(MBeanRequestHandler mBeanRequestHandler) {
        this.mBeanRequestHandler = mBeanRequestHandler;
    }

    public CollectionsAPIOverseerStatus getCollectionsAPIOverseerStatus() {
        return collectionsAPIOverseerStatus;
    }

    public void setCollectionsAPIOverseerStatus(CollectionsAPIOverseerStatus collectionsAPIOverseerStatus) {
        this.collectionsAPIOverseerStatus = collectionsAPIOverseerStatus;
    }

    public CollectionsAPIClusterStatus getCollectionsAPIClusterStatus() {
        return collectionsAPIClusterStatus;
    }

    public void setCollectionsAPIClusterStatus(CollectionsAPIClusterStatus collectionsAPIClusterStatus) {
        this.collectionsAPIClusterStatus = collectionsAPIClusterStatus;
    }

    public MetricsReporting getMetricsReporting() {
        return metricsReporting;
    }

    public void setMetricsReporting(MetricsReporting metricsReporting) {
        this.metricsReporting = metricsReporting;
    }

    public Facet getFacet() {
        return facet;
    }

    public void setFacet(Facet facet) {
        this.facet = facet;
    }
}
