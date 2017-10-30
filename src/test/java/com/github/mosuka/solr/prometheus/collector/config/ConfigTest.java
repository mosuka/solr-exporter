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
import junit.framework.TestCase;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.FileReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Unit test for Config.
 */
public class ConfigTest extends TestCase {
    @Test
    public void testLoadFile() throws Exception {
        String configFile = "src/test/files/conf/config.yml";

        Config config = new Yaml().loadAs(new FileReader(configFile), Config.class);

        assertNotNull(config);
    }

//    @Test
//    public void testGetBaseUrl() throws Exception {
//        String configFile = "src/test/files/conf/config.yml";
//
//        Config config = new Yaml().loadAs(new FileReader(configFile), Config.class);
//
//        String expected = config.getBaseUrl();
//        String actual = "";
//        assertEquals(expected, actual);
//    }

//    @Test
//    public void testSetBaseUrl() throws Exception {
//        String configFile = "src/test/files/conf/config.yml";
//
//        Config config = new Yaml().loadAs(new FileReader(configFile), Config.class);
//
//        config.setBaseUrl("http://localhost:8984/solr");
//
//        String expected = "http://localhost:8984/solr";
//        String actual = config.getBaseUrl();
//        assertEquals(expected, actual);
//    }

//    @Test
//    public void testGetZkHosts() throws Exception {
//        String configFile = "src/test/files/conf/config.yml";
//
//        Config config = new Yaml().loadAs(new FileReader(configFile), Config.class);
//
//        List<String> expected = Collections.singletonList("localhost:2181");
//        List<String> actual = config.getZkHosts();
//        assertEquals(expected, actual);
//    }

//    @Test
//    public void testSetZkHosts() throws Exception {
//        String configFile = "src/test/files/conf/config.yml";
//
//        Config config = new Yaml().loadAs(new FileReader(configFile), Config.class);
//
//        config.setZkHosts(Arrays.asList("localhost:2181", "localhost:2182", "localhost:2183"));
//
//        List<String> expected = Arrays.asList("localhost:2181", "localhost:2182", "localhost:2183");
//        List<String> actual = config.getZkHosts();
//        assertEquals(expected, actual);
//    }

//    @Test
//    public void testGetZnode() throws Exception {
//        String configFile = "src/test/files/conf/config.yml";
//
//        Config config = new Yaml().loadAs(new FileReader(configFile), Config.class);
//
//        String expected = "/solr";
//        String actual = config.getZnode();
//        assertEquals(expected, actual);
//    }

//    @Test
//    public void testSetZnode() throws Exception {
//        String configFile = "src/test/files/conf/config.yml";
//
//        Config config = new Yaml().loadAs(new FileReader(configFile), Config.class);
//
//        config.setZnode("/solr2");
//
//        String expected = "/solr2";
//        String actual = config.getZnode();
//        assertEquals(expected, actual);
//    }

//    @Test
//    public void testGetPing() throws Exception {
//        String configFile = "src/test/files/conf/config.yml";
//
//        Config config = new Yaml().loadAs(new FileReader(configFile), Config.class);
//
//        Ping ping = new Ping();
//        ping.setEnable(true);
//
//        Ping expected = ping;
//        Ping actual = config.getPing();
//
//        assertEquals(expected.getEnable(), actual.getEnable());
//        assertEquals(expected.getCores(), actual.getCores());
//    }

//    @Test
//    public void testSetPing() throws Exception {
//        String configFile = "src/test/files/conf/config.yml";
//
//        Config config = new Yaml().loadAs(new FileReader(configFile), Config.class);
//
//        Ping ping = new Ping();
//        ping.setEnable(false);
//
//        config.setPing(ping);
//
//        Ping expected = ping;
//        Ping actual = config.getPing();
//
//        assertEquals(expected.getEnable(), actual.getEnable());
//        assertEquals(expected.getCores(), actual.getCores());
//    }

//    @Test
//    public void testGetCoreAdminAPIStatus() throws Exception {
//        String configFile = "src/test/files/conf/config.yml";
//
//        Config config = new Yaml().loadAs(new FileReader(configFile), Config.class);
//
//        CoreAdminAPIStatus coreAdminAPIStatus = new CoreAdminAPIStatus();
//        coreAdminAPIStatus.setEnable(true);
//
//        CoreAdminAPIStatus expected = coreAdminAPIStatus;
//        CoreAdminAPIStatus actual = config.getCoreAdminAPIStatus();
//
//        assertEquals(expected.getEnable(), actual.getEnable());
//        assertEquals(expected.getCores(), actual.getCores());
//    }

//    @Test
//    public void testSetCoreAdminAPIStatus() throws Exception {
//        String configFile = "src/test/files/conf/config.yml";
//
//        Config config = new Yaml().loadAs(new FileReader(configFile), Config.class);
//
//        CoreAdminAPIStatus coreAdminAPIStatus = new CoreAdminAPIStatus();
//        coreAdminAPIStatus.setEnable(false);
//
//        config.setCoreAdminAPIStatus(coreAdminAPIStatus);
//
//        CoreAdminAPIStatus expected = coreAdminAPIStatus;
//        CoreAdminAPIStatus actual = config.getCoreAdminAPIStatus();
//
//        assertEquals(expected.getEnable(), actual.getEnable());
//        assertEquals(expected.getCores(), actual.getCores());
//    }

//    @Test
//    public void testGetMBeanRequestHandler() throws Exception {
//        String configFile = "src/test/files/conf/config.yml";
//
//        Config config = new Yaml().loadAs(new FileReader(configFile), Config.class);
//
//        MBeanRequestHandler MBeanRequestHandler = new MBeanRequestHandler();
//        MBeanRequestHandler.setEnable(true);
//
//        MBeanRequestHandler expected = MBeanRequestHandler;
//        MBeanRequestHandler actual = config.getmBeanRequestHandler();
//
//        assertEquals(expected.getEnable(), actual.getEnable());
//        assertEquals(expected.getCores(), actual.getCores());
//    }

//    @Test
//    public void testSetMBeanRequestHandler() throws Exception {
//        String configFile = "src/test/files/conf/config.yml";
//
//        Config config = new Yaml().loadAs(new FileReader(configFile), Config.class);
//
//        MBeanRequestHandler MBeanRequestHandler = new MBeanRequestHandler();
//        MBeanRequestHandler.setEnable(false);
//
//        config.setmBeanRequestHandler(MBeanRequestHandler);
//
//        MBeanRequestHandler expected = MBeanRequestHandler;
//        MBeanRequestHandler actual = config.getmBeanRequestHandler();
//
//        assertEquals(expected.getEnable(), actual.getEnable());
//        assertEquals(expected.getCores(), actual.getCores());
//    }

//    @Test
//    public void testGetCollectionAPIOverseerStatus() throws Exception {
//        String configFile = "src/test/files/conf/config.yml";
//
//        Config config = new Yaml().loadAs(new FileReader(configFile), Config.class);
//
//        CollectionsAPIOverseerStatus collectionsAPIOverseerStatus = new CollectionsAPIOverseerStatus();
//        collectionsAPIOverseerStatus.setEnable(true);
//
//        CollectionsAPIOverseerStatus expected = collectionsAPIOverseerStatus;
//        CollectionsAPIOverseerStatus actual = config.getCollectionsAPIOverseerStatus();
//
//        assertEquals(expected.getEnable(), actual.getEnable());
//    }

//    @Test
//    public void testSetCollectionAPIOverseerStatus() throws Exception {
//        String configFile = "src/test/files/conf/config.yml";
//
//        Config config = new Yaml().loadAs(new FileReader(configFile), Config.class);
//
//        CollectionsAPIOverseerStatus collectionsAPIOverseerStatus = new CollectionsAPIOverseerStatus();
//        collectionsAPIOverseerStatus.setEnable(false);
//
//        config.setCollectionsAPIOverseerStatus(collectionsAPIOverseerStatus);
//
//        CollectionsAPIOverseerStatus expected = collectionsAPIOverseerStatus;
//        CollectionsAPIOverseerStatus actual = config.getCollectionsAPIOverseerStatus();
//
//        assertEquals(expected.getEnable(), actual.getEnable());
//    }

//    @Test
//    public void testGetCollectionAPIClusterStatus() throws Exception {
//        String configFile = "src/test/files/conf/config.yml";
//
//        Config config = new Yaml().loadAs(new FileReader(configFile), Config.class);
//
//        CollectionsAPIClusterStatus collectionsAPIClusterStatus = new CollectionsAPIClusterStatus();
//        collectionsAPIClusterStatus.setEnable(true);
//
//        CollectionsAPIClusterStatus expected = collectionsAPIClusterStatus;
//        CollectionsAPIClusterStatus actual = config.getCollectionsAPIClusterStatus();
//
//        assertEquals(expected.getEnable(), actual.getEnable());
//        assertEquals(expected.getCollections(), actual.getCollections());
//    }

//    @Test
//    public void testSetCollectionAPIClusterStatus() throws Exception {
//        String configFile = "src/test/files/conf/config.yml";
//
//        Config config = new Yaml().loadAs(new FileReader(configFile), Config.class);
//
//        CollectionsAPIClusterStatus collectionsAPIClusterStatus = new CollectionsAPIClusterStatus();
//        collectionsAPIClusterStatus.setEnable(false);
//
//        config.setCollectionsAPIClusterStatus(collectionsAPIClusterStatus);
//
//        CollectionsAPIClusterStatus expected = collectionsAPIClusterStatus;
//        CollectionsAPIClusterStatus actual = config.getCollectionsAPIClusterStatus();
//
//        assertEquals(expected.getEnable(), actual.getEnable());
//        assertEquals(expected.getCollections(), actual.getCollections());
//    }

//    @Test
//    public void testGetMetricsReporting() throws Exception {
//        String configFile = "src/test/files/conf/config.yml";
//
//        Config config = new Yaml().loadAs(new FileReader(configFile), Config.class);
//
//        MetricsReporting metricsReporting = new MetricsReporting();
//        metricsReporting.setEnable(true);
//
//        MetricsReporting expected = metricsReporting;
//        MetricsReporting actual = config.getMetricsReporting();
//
//        assertEquals(expected.getEnable(), actual.getEnable());
//        assertEquals(expected.getGroup(), actual.getGroup());
//        assertEquals(expected.getType(), actual.getType());
//        assertEquals(expected.getPrefix(), actual.getPrefix());
//    }

//    @Test
//    public void testSetMetricsReporting() throws Exception {
//        String configFile = "src/test/files/conf/config.yml";
//
//        Config config = new Yaml().loadAs(new FileReader(configFile), Config.class);
//
//        MetricsReporting metricsReporting = new MetricsReporting();
//        metricsReporting.setEnable(false);
//
//        config.setMetricsReporting(metricsReporting);
//
//        MetricsReporting expected = metricsReporting;
//        MetricsReporting actual = config.getMetricsReporting();
//
//        assertEquals(expected.getEnable(), actual.getEnable());
//        assertEquals(expected.getGroup(), actual.getGroup());
//        assertEquals(expected.getType(), actual.getType());
//        assertEquals(expected.getPrefix(), actual.getPrefix());
//    }

//    @Test
//    public void testGetFacet() throws Exception {
//        String configFile = "src/test/files/conf/config.yml";
//
//        Config config = new Yaml().loadAs(new FileReader(configFile), Config.class);
//
//        Facet facet = new Facet();
//        facet.setEnable(true);
//
//        Facet expected = facet;
//        Facet actual = config.getFacet();
//
//        assertEquals(expected.getEnable(), actual.getEnable());
//    }

//    @Test
//    public void testSetFacet() throws Exception {
//        String configFile = "src/test/files/conf/config.yml";
//
//        Config config = new Yaml().loadAs(new FileReader(configFile), Config.class);
//
//        Facet facet = new Facet();
//        facet.setEnable(false);
//
//        config.setFacet(facet);
//
//        Facet expected = facet;
//        Facet actual = config.getFacet();
//
//        assertEquals(expected.getEnable(), actual.getEnable());
//    }
}
