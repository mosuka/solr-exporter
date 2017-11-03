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
package com.github.mosuka.solr.prometheus.scraper.config;

import com.github.mosuka.solr.prometheus.collector.config.CollectorConfig;
import junit.framework.TestCase;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit test for ScraperConfig.
 */
public class ScraperConfigTest extends TestCase {
    @Test
    public void testScraperConfig() throws Exception {
        String configFile = "conf/config.yml";

        CollectorConfig collectorConfig = new Yaml().loadAs(new FileReader(configFile), CollectorConfig.class);

        ScraperConfig scraperConfig = collectorConfig.getPing();

        assertNotNull(scraperConfig);
    }

    @Test
    public void testGetJsonQueries() throws Exception {
        String configFile = "conf/config.yml";

        CollectorConfig collectorConfig = new Yaml().loadAs(new FileReader(configFile), CollectorConfig.class);

        ScraperConfig scraperConfig = collectorConfig.getPing();

        assertNotNull(scraperConfig.getJsonQueries());
    }

    @Test
    public void testSetJsonQueries() throws Exception {
        List<String> jsonQueries = new ArrayList<>();

        ScraperConfig scraperConfig = new ScraperConfig();

        scraperConfig.setJsonQueries(jsonQueries);

        assertNotNull(scraperConfig.getJsonQueries());
    }

    @Test
    public void testGetQueryConfig() throws Exception {
        String configFile = "conf/config.yml";

        CollectorConfig collectorConfig = new Yaml().loadAs(new FileReader(configFile), CollectorConfig.class);

        ScraperConfig scraperConfig = collectorConfig.getPing();

        assertNotNull(scraperConfig.getQuery());
    }

    @Test
    public void testSetQueryConfig() throws Exception {
        QueryConfig queryConfig = new QueryConfig();

        ScraperConfig scraperConfig = new ScraperConfig();

        scraperConfig.setQuery(queryConfig);

        assertNotNull(scraperConfig.getQuery());
    }
}
