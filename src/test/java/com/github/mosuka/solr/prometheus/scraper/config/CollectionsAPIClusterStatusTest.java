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

import com.github.mosuka.solr.prometheus.scraper.config.CollectionsAPIClusterStatus;
import junit.framework.TestCase;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Unit test for CollectionsAPIClusterStatus.
 */
public class CollectionsAPIClusterStatusTest extends TestCase {

    @Test
    public void testCollectionsAPIClusterStatus() throws Exception {
        CollectionsAPIClusterStatus collectionsAPIClusterStatus = new CollectionsAPIClusterStatus();

        assertNotNull(collectionsAPIClusterStatus);
    }

    @Test
    public void testGetEnable() throws Exception {
        CollectionsAPIClusterStatus collectionsAPIClusterStatus = new CollectionsAPIClusterStatus();

        boolean expected = false;
        boolean actual = collectionsAPIClusterStatus.getEnable();
        assertEquals(expected, actual);
    }

    @Test
    public void testSetEnable() throws Exception {
        CollectionsAPIClusterStatus collectionsAPIClusterStatus = new CollectionsAPIClusterStatus();

        collectionsAPIClusterStatus.setEnable(true);

        boolean expected = true;
        boolean actual = collectionsAPIClusterStatus.getEnable();
        assertEquals(expected, actual);
    }

    @Test
    public void testGetCollections() throws Exception {
        CollectionsAPIClusterStatus collectionsAPIClusterStatus = new CollectionsAPIClusterStatus();

        List<String> expected = new ArrayList<>();
        List<String> actual = collectionsAPIClusterStatus.getCollections();
        assertEquals(expected, actual);
    }

    @Test
    public void testSetCollections() throws Exception {
        CollectionsAPIClusterStatus collectionsAPIClusterStatus = new CollectionsAPIClusterStatus();

        collectionsAPIClusterStatus.setCollections(Arrays.asList("collection1", "collection2"));

        List<String> expected = Arrays.asList("collection1", "collection2");
        List<String> actual = collectionsAPIClusterStatus.getCollections();
        assertEquals(expected, actual);
    }
}
