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

import com.github.mosuka.solr.prometheus.scraper.config.CollectionsAPIOverseerStatus;
import junit.framework.TestCase;
import org.junit.Test;

/**
 * Unit test for CollectionsAPIOverseerStatus.
 */
public class CollectionsAPIOverseerStatusTest extends TestCase {

    @Test
    public void testCollectionsAPIOverseerStatus() throws Exception {
        CollectionsAPIOverseerStatus collectionsAPIOverseerStatus = new CollectionsAPIOverseerStatus();

        assertNotNull(collectionsAPIOverseerStatus);
    }

    @Test
    public void testGetEnable() throws Exception {
        CollectionsAPIOverseerStatus collectionsAPIOverseerStatus = new CollectionsAPIOverseerStatus();

        boolean expected = false;
        boolean actual = collectionsAPIOverseerStatus.getEnable();
        assertEquals(expected, actual);
    }

    @Test
    public void testSetEnable() throws Exception {
        CollectionsAPIOverseerStatus collectionsAPIOverseerStatus = new CollectionsAPIOverseerStatus();

        collectionsAPIOverseerStatus.setEnable(true);

        boolean expected = true;
        boolean actual = collectionsAPIOverseerStatus.getEnable();
        assertEquals(expected, actual);
    }
}
