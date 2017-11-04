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
package com.github.mosuka.solr.prometheus.collector;

import com.github.mosuka.solr.prometheus.collector.config.SolrCollectorConfig;
import com.github.mosuka.solr.prometheus.exporter.SolrExporterTestBase;
import io.prometheus.client.CollectorRegistry;
import org.apache.lucene.util.LuceneTestCase.Slow;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.List;

/**
 * Unit test for SolrCollector.
 */
@Slow
public class SolrCollectorTest extends SolrExporterTestBase {
    CollectorRegistry registry;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        registry = new CollectorRegistry();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testSolrCollector() throws Exception {
        String configFile = "conf/config.yml";

        CloudSolrClient cloudSolrClient = cluster.getSolrClient();
        SolrCollectorConfig collectorConfig = new Yaml().loadAs(new FileReader(configFile), SolrCollectorConfig.class);

        SolrCollector collector = new SolrCollector(cloudSolrClient, collectorConfig, 1);

        assertNotNull(collector);

    }

    @Test
    public void testCollectPing() throws Exception {
        String configFile = "conf/config.yml";

        CloudSolrClient cloudSolrClient = cluster.getSolrClient();
        SolrCollectorConfig collectorConfig = new Yaml().loadAs(new FileReader(configFile), SolrCollectorConfig.class);

        SolrCollector collector = new SolrCollector(cloudSolrClient, collectorConfig, 1);
        collector.register(registry);

        // index sample docs
        File exampleDocsDir = new File("src/test/files/solr/example/exampledocs");
        List<File> xmlFiles = Arrays.asList(exampleDocsDir.listFiles((dir, name) -> name.endsWith(".xml")));
        for (File xml : xmlFiles) {
            ContentStreamUpdateRequest req = new ContentStreamUpdateRequest("/update");
            req.addFile(xml, "application/xml");
            cloudSolrClient.request(req, "collection1");
        }
        cloudSolrClient.commit("collection1");

        collector.collect();

        assertNotEquals(0.0, registry.getSampleValue("solr_scrape_duration_seconds"));

    }
}

