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

import com.github.mosuka.solr.prometheus.scraper.config.Query;
import junit.framework.TestCase;
import org.junit.Test;

import java.util.*;

public class QueryTest extends TestCase {
    @Test
    public void testQuery() throws Exception {
        Query query = new Query();

        assertNotNull(query);
    }

    @Test
    public void testGetCollection() throws Exception {
        Query query = new Query();

        String expected = "";
        String actual = query.getCollection();
        assertEquals(expected, actual);
    }

    @Test
    public void testSetCollection() throws Exception {
        Query query = new Query();

        query.setCollection("collection1");

        String expected = "collection1";
        String actual = query.getCollection();
        assertEquals(expected, actual);
    }

    @Test
    public void testGetPath() throws Exception {
        Query query = new Query();

        String expected = "";
        String actual = query.getPath();
        assertEquals(expected, actual);
    }

    @Test
    public void testSetPath() throws Exception {
        Query query = new Query();

        query.setPath("/select");

        String expected = "/select";
        String actual = query.getPath();
        assertEquals(expected, actual);
    }

    @Test
    public void testGetParams() throws Exception {
        Query query = new Query();

        List<LinkedHashMap<String, String>> expected = new ArrayList<>();
        List<LinkedHashMap<String, String>> actual = query.getParams();
        assertEquals(expected, actual);
    }

    @Test
    public void testSetParams() throws Exception {
        Query query = new Query();

        LinkedHashMap<String,String> param1 = new LinkedHashMap<>();
        param1.put("q", "*:*");

        LinkedHashMap<String,String> param2 = new LinkedHashMap<>();
        param2.put("facet", "on");

        query.setParams(Arrays.asList(param1, param2));

        List<LinkedHashMap<String, String>> expected = Arrays.asList(param1, param2);
        List<LinkedHashMap<String, String>> actual = query.getParams();
        assertEquals(expected, actual);
    }

    @Test
    public void testGetParamsString() throws Exception {
        Query query = new Query();

        LinkedHashMap<String,String> param1 = new LinkedHashMap<>();
        param1.put("q", "*:*");
        param1.put("fq", "manu:apple");

        LinkedHashMap<String,String> param2 = new LinkedHashMap<>();
        param2.put("facet", "on");

        query.setParams(Arrays.asList(param1, param2));

        String expected = "q=*:*&fq=manu:apple&facet=on";
        String actual = query.getParamsString();
        assertEquals(expected, actual);
    }
}
