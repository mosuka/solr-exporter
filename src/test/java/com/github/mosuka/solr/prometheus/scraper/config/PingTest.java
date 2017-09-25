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

import com.github.mosuka.solr.prometheus.scraper.config.Ping;
import junit.framework.TestCase;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Unit test for Ping.
 */
public class PingTest extends TestCase {
    @Test
    public void testPing() throws Exception {
        Ping ping = new Ping();

        assertNotNull(ping);
    }

    @Test
    public void testGetEnable() throws Exception {
        Ping ping = new Ping();

        boolean expected = false;
        boolean actual = ping.getEnable();
        assertEquals(expected, actual);
    }

    @Test
    public void testSetEnable() throws Exception {
        Ping ping = new Ping();

        ping.setEnable(true);
        boolean expected = true;
        boolean actual = ping.getEnable();
        assertEquals(expected, actual);
    }

    @Test
    public void testGetCores() throws Exception {
        Ping ping = new Ping();

        List<String> expected = new ArrayList<>();
        List<String> actual = ping.getCores();
        assertEquals(expected, actual);
    }

    @Test
    public void testSetCores() throws Exception {
        Ping ping = new Ping();

        ping.setCores(Arrays.asList("core1", "core2"));

        List<String> expected = Arrays.asList("core1", "core2");
        List<String> actual = ping.getCores();
        assertEquals(expected, actual);
    }
}
