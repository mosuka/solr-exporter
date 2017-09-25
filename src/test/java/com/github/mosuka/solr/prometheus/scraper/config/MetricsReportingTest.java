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

import com.github.mosuka.solr.prometheus.scraper.config.MetricsReporting;
import junit.framework.TestCase;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Unit test for Ping.
 */
public class MetricsReportingTest extends TestCase {

    @Test
    public void testMetricsReporting() throws Exception {
        MetricsReporting metricsReporting = new MetricsReporting();

        assertNotNull(metricsReporting);
    }

    @Test
    public void testGetEnable() throws Exception {
        MetricsReporting metricsReporting = new MetricsReporting();

        boolean expected = false;
        boolean actual = metricsReporting.getEnable();
        assertEquals(expected, actual);
    }

    @Test
    public void testSetEnable() throws Exception {
        MetricsReporting metricsReporting = new MetricsReporting();

        metricsReporting.setEnable(true);

        boolean expected = true;
        boolean actual = metricsReporting.getEnable();
        assertEquals(expected, actual);
    }

    @Test
    public void testGetGroup() throws Exception {
        MetricsReporting metricsReporting = new MetricsReporting();

        List<String> expected = new ArrayList<>();
        List<String> actual = metricsReporting.getGroup();
        assertEquals(expected, actual);
    }

    @Test
    public void testSetGroup() throws Exception {
        MetricsReporting metricsReporting = new MetricsReporting();

        metricsReporting.setGroup(Arrays.asList("jvm", "node"));

        List<String> expectedGroup = Arrays.asList("jvm", "node");
        List<String> actualGroup = metricsReporting.getGroup();
        assertEquals(expectedGroup, actualGroup);
    }

    @Test
    public void testGetType() throws Exception {
        MetricsReporting metricsReporting = new MetricsReporting();

        List<String> expected = new ArrayList<>();
        List<String> actual = metricsReporting.getType();
        assertEquals(expected, actual);
    }

    @Test
    public void testSetType() throws Exception {
        MetricsReporting metricsReporting = new MetricsReporting();

        metricsReporting.setType(Arrays.asList("counter", "gauge"));

        List<String> expected = Arrays.asList("counter", "gauge");
        List<String> actual = metricsReporting.getType();
        assertEquals(expected, actual);
    }

    @Test
    public void testGetPrefix() throws Exception {
        MetricsReporting metricsReporting = new MetricsReporting();

        List<String> expected = new ArrayList<>();
        List<String> actual = metricsReporting.getPrefix();
        assertEquals(expected, actual);
    }

    @Test
    public void testSetPrefix() throws Exception {
        MetricsReporting metricsReporting = new MetricsReporting();

        metricsReporting.setPrefix(Arrays.asList("ADMIN", "QUERY"));

        List<String> expected = Arrays.asList("ADMIN", "QUERY");
        List<String> actual = metricsReporting.getPrefix();
        assertEquals(expected, actual);
    }
}
