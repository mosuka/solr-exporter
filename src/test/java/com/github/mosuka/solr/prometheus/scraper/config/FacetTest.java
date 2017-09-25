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

import com.github.mosuka.solr.prometheus.scraper.config.Facet;
import com.github.mosuka.solr.prometheus.scraper.config.Query;
import junit.framework.TestCase;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

public class FacetTest extends TestCase {
    @Test
    public void testFacet() throws Exception {
        Facet facet = new Facet();

        assertNotNull(facet);
    }

    @Test
    public void testGetEnable() throws Exception {
        Facet facet = new Facet();

        boolean expected = false;
        boolean actual = facet.getEnable();
        assertEquals(expected, actual);
    }

    @Test
    public void testSetEnable() throws Exception {
        Facet facet = new Facet();

        facet.setEnable(true);

        boolean expected = true;
        boolean actual = facet.getEnable();
        assertEquals(expected, actual);
    }

    @Test
    public void testGetQuery() throws Exception {
        Facet facet = new Facet();

        List<Query> expected = new ArrayList<>();
        List<Query> actual = facet.getQueries();
        assertEquals(expected, actual);
    }

    @Test
    public void testSetQuery() throws Exception {
        Facet facet = new Facet();

        Query query1 = new Query();
        query1.setCollection("collection1");
        query1.setPath("/select");

        LinkedHashMap<String,String> param1 = new LinkedHashMap<>();
        param1.put("q", "*:*");
        LinkedHashMap<String,String> param2 = new LinkedHashMap<>();
        param2.put("facet", "on");

        query1.setParams(Arrays.asList(param1, param2));

        Query query2 = new Query();
        query2.setCollection("collection1");
        query2.setPath("/select");

        LinkedHashMap<String,String> param3 = new LinkedHashMap<>();
        param3.put("q", "*:*");
        LinkedHashMap<String,String> param4 = new LinkedHashMap<>();
        param4.put("facet", "on");

        query2.setParams(Arrays.asList(param3, param4));

        facet.setQueries(Arrays.asList(query1, query2));

        List<Query> expected = Arrays.asList(query1, query2);
        List<Query> actual = facet.getQueries();
        assertEquals(expected, actual);
    }
}
