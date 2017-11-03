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

import com.github.mosuka.solr.prometheus.scraper.config.ScraperConfig;
import junit.framework.TestCase;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit test for CollectorConfig.
 */
public class CollectorConfigTest extends TestCase {
    @Test
    public void testCollectorConfig() throws Exception {
        String configFile = "conf/config.yml";

        CollectorConfig collectorConfig = new Yaml().loadAs(new FileReader(configFile), CollectorConfig.class);

        assertNotNull(collectorConfig);
    }

    @Test
    public void testGetPingConfig() throws Exception {
        String configFile = "conf/config.yml";

        CollectorConfig collectorConfig = new Yaml().loadAs(new FileReader(configFile), CollectorConfig.class);

        assertNotNull(collectorConfig.getPing());
    }

    @Test
    public void testSetPingConfig() throws Exception {
        String configFile = "conf/config.yml";

        CollectorConfig collectorConfig = new Yaml().loadAs(new FileReader(configFile), CollectorConfig.class);

        ScraperConfig pingConfig = new ScraperConfig();

        collectorConfig.setPing(pingConfig);

        assertNotNull(collectorConfig.getPing());
    }

    @Test
    public void testGetMetricsConfig() throws Exception {
        String configFile = "conf/config.yml";

        CollectorConfig collectorConfig = new Yaml().loadAs(new FileReader(configFile), CollectorConfig.class);

        assertNotNull(collectorConfig.getMetrics());
    }

    @Test
    public void testSetMetricsConfig() throws Exception {
        String configFile = "conf/config.yml";

        CollectorConfig collectorConfig = new Yaml().loadAs(new FileReader(configFile), CollectorConfig.class);

        ScraperConfig metricsConfig = new ScraperConfig();

        collectorConfig.setMetrics(metricsConfig);

        assertNotNull(collectorConfig.getMetrics());
    }

    @Test
    public void testGetCollectionsConfig() throws Exception {
        String configFile = "conf/config.yml";

        CollectorConfig collectorConfig = new Yaml().loadAs(new FileReader(configFile), CollectorConfig.class);

        assertNotNull(collectorConfig.getCollections());
    }

    @Test
    public void testSetCollectionsConfig() throws Exception {
        String configFile = "conf/config.yml";

        CollectorConfig collectorConfig = new Yaml().loadAs(new FileReader(configFile), CollectorConfig.class);

        ScraperConfig collectionsConfig = new ScraperConfig();

        collectorConfig.setCollections(collectionsConfig);

        assertNotNull(collectorConfig.getCollections());
    }

    @Test
    public void testGetQueryConfigs() throws Exception {
        String configFile = "conf/config.yml";

        CollectorConfig collectorConfig = new Yaml().loadAs(new FileReader(configFile), CollectorConfig.class);

        assertNotNull(collectorConfig.getQueries());
    }

    @Test
    public void testSetQueryConfigs() throws Exception {
        String configFile = "conf/config.yml";

        CollectorConfig collectorConfig = new Yaml().loadAs(new FileReader(configFile), CollectorConfig.class);

        List<ScraperConfig> queryConfigs = new ArrayList<>();

        collectorConfig.setQueries(queryConfigs);

        assertNotNull(collectorConfig.getQueries());
    }
}
