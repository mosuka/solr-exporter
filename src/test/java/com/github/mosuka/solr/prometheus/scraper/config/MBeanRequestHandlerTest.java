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

import com.github.mosuka.solr.prometheus.scraper.config.MBeanRequestHandler;
import junit.framework.TestCase;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Unit test for MBeanRequestHandler.
 */
public class MBeanRequestHandlerTest extends TestCase {

    @Test
    public void testMBeanRequestHandler() throws Exception {
        MBeanRequestHandler MBeanRequestHandler = new MBeanRequestHandler();

        assertNotNull(MBeanRequestHandler);
    }

    @Test
    public void testGetEnable() throws Exception {
        MBeanRequestHandler MBeanRequestHandler = new MBeanRequestHandler();

        boolean expected = false;
        boolean actual = MBeanRequestHandler.getEnable();
        assertEquals(expected, actual);
    }

    @Test
    public void testSetEnable() throws Exception {
        MBeanRequestHandler MBeanRequestHandler = new MBeanRequestHandler();

        MBeanRequestHandler.setEnable(true);

        boolean expected = true;
        boolean actual = MBeanRequestHandler.getEnable();
        assertEquals(expected, actual);
    }

    @Test
    public void testGetCores() throws Exception {
        MBeanRequestHandler MBeanRequestHandler = new MBeanRequestHandler();

        List<String> expected = new ArrayList<>();
        List<String> actual = MBeanRequestHandler.getCores();
        assertEquals(expected, actual);
    }

    @Test
    public void testSetCores() throws Exception {
        MBeanRequestHandler MBeanRequestHandler = new MBeanRequestHandler();

        MBeanRequestHandler.setCores(Arrays.asList("core1", "core2"));

        List<String> expected = Arrays.asList("core1", "core2");
        List<String> actual = MBeanRequestHandler.getCores();
        assertEquals(expected, actual);
    }

    @Test
    public void testGetCat() throws Exception {
        MBeanRequestHandler MBeanRequestHandler = new MBeanRequestHandler();

        String expected = "";
        String actual = MBeanRequestHandler.getCat();
        assertEquals(expected, actual);
    }

    @Test
    public void testSetCat() throws Exception {
        MBeanRequestHandler MBeanRequestHandler = new MBeanRequestHandler();

        MBeanRequestHandler.setCat("ADMIN");

        String expected = "ADMIN";
        String actual = MBeanRequestHandler.getCat();
        assertEquals(expected, actual);
    }

    @Test
    public void testGetKey() throws Exception {
        MBeanRequestHandler MBeanRequestHandler = new MBeanRequestHandler();

        String expected = "";
        String actual = MBeanRequestHandler.getKey();
        assertEquals(expected, actual);
    }

    @Test
    public void testSetKey() throws Exception {
        MBeanRequestHandler MBeanRequestHandler = new MBeanRequestHandler();

        MBeanRequestHandler.setKey("/admin/mbeans");

        String expected = "/admin/mbeans";
        String actual = MBeanRequestHandler.getKey();
        assertEquals(expected, actual);
    }
}
