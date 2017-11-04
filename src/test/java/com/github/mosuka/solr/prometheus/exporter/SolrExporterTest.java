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
package com.github.mosuka.solr.prometheus.exporter;

import com.github.mosuka.solr.prometheus.collector.config.SolrCollectorConfig;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.lucene.util.LuceneTestCase.Slow;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.FileReader;
import java.net.*;

/**
 * Unit test for SolrExporter.
 */
@Slow
public class SolrExporterTest extends SolrExporterTestBase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testExecuteStandalone() throws Exception {
        String configFile = "conf/config.yml";

        SolrCollectorConfig collectorConfig = new Yaml().loadAs(new FileReader(configFile), SolrCollectorConfig.class);

        // solr client
        CloudSolrClient cloudSolrClient = cluster.getSolrClient();

        int port;
        ServerSocket socket = null;
        try {
            socket = new ServerSocket(0);
            port = socket.getLocalPort();
        } finally {
            socket.close();
        }


        SolrExporter solrExporter = new SolrExporter(port, cloudSolrClient, collectorConfig, 1);
        try {
            solrExporter.start();

            URI uri = new URI("http://localhost:" + String.valueOf(port) + "/metrics");

            CloseableHttpClient httpclient = HttpClients.createDefault();
            CloseableHttpResponse response = null;
            try {
                HttpGet request = new HttpGet(uri);
                response = httpclient.execute(request);

                int expectedHTTPStatusCode = HttpStatus.SC_OK;
                int actualHTTPStatusCode = response.getStatusLine().getStatusCode();
                assertEquals(expectedHTTPStatusCode, actualHTTPStatusCode);

            } finally {
                response.close();
                httpclient.close();
            }
        } finally {
            solrExporter.stop();
        }
    }

    @Test
    public void testExecuteSolrCloud() throws Exception {
        String configFile = "conf/config.yml";

        SolrCollectorConfig collectorConfig = new Yaml().loadAs(new FileReader(configFile), SolrCollectorConfig.class);

        // solr client
        CloudSolrClient cloudSolrClient = cluster.getSolrClient();

        int port;
        ServerSocket socket = null;
        try {
            socket = new ServerSocket(0);
            port = socket.getLocalPort();
        } finally {
            socket.close();
        }

        SolrExporter solrExporter = new SolrExporter(port, cloudSolrClient, collectorConfig, 1);
        try {
            solrExporter.start();

            URI uri = new URI("http://localhost:" + String.valueOf(port) + "/metrics");

            CloseableHttpClient httpclient = HttpClients.createDefault();
            CloseableHttpResponse response = null;
            try {
                HttpGet request = new HttpGet(uri);
                response = httpclient.execute(request);

                int expectedHTTPStatusCode = HttpStatus.SC_OK;
                int actualHTTPStatusCode = response.getStatusLine().getStatusCode();
                assertEquals(expectedHTTPStatusCode, actualHTTPStatusCode);

            } finally {
                response.close();
                httpclient.close();
            }
        } finally {
            solrExporter.stop();
        }
    }
}
