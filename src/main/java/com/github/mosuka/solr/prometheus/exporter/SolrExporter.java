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

import com.github.mosuka.solr.prometheus.collector.SolrCollector;
import com.github.mosuka.solr.prometheus.collector.config.Config;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.HTTPServer;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.NoOpResponseParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import javax.management.MalformedObjectNameException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SolrExporter
 *
 */
public class SolrExporter {
    private static final Logger logger = LoggerFactory.getLogger(SolrCollector.class);

    /**
     * -v, --version
     */
    private static final String[] ARG_VERSION_FLAGS = { "-v", "--version" };
    private static final String ARG_VERSION_DEST = "version";
    private static final String ARG_VERSION_HELP = "Show version.";
    public static String VERSION = "unknown";

    /**
     * -p, --port
     */
    private static final String[] ARG_PORT_FLAGS = { "-p", "--port" };
    private static final String ARG_PORT_METAVAR = "PORT";
    private static final String ARG_PORT_DEST = "port";
    private static final Integer ARG_PORT_DEFAULT = 9983;
    private static final String ARG_PORT_HELP = "solr-exporter listen port.";

    /**
     * -s, --solrurl
     */
    private static final String[] ARG_SOLR_URL_FLAGS = { "-s", "--solrurl" };
    private static final String ARG_SOLR_URL_METAVAR = "SOLR_URL";
    private static final String ARG_SOLR_URL_DEST = "solrUrl";
    private static final String ARG_SOLR_URL_DEFAULT = "";
    private static final String ARG_SOLR_URL_HELP = "Solr base url (ex: 'http://localhost:8983/solr'). Specify this when connecting to Solr in standalone mode.";

    /**
     * -z, --zkhost
     */
    private static final String[] ARG_ZK_HOST_FLAGS = { "-z", "--zkhost" };
    private static final String ARG_ZK_HOST_METAVAR = "ZK_HOST";
    private static final String ARG_ZK_HOST_DEST = "zkHost";
    private static final String ARG_ZK_HOST_DEFAULT = "";
    private static final String ARG_ZK_HOST_HELP = "ZooKeeper connection string (ex: 'localhost:2181/solr'). Specify this when connecting to a Solr cluster in SolrCloud mode.";

    /**
     * -c, --config
     */
    private static final String[] ARG_CONFIG_FLAGS = { "-c", "--config" };
    private static final String ARG_CONFIG_METAVAR = "CONFIG";
    private static final String ARG_CONFIG_DEST = "config";
    private static final String ARG_CONFIG_DEFAULT = "./conf/config.yml";
    private static final String ARG_CONFIG_HELP = "Configuration file.";

    private int port;
    private Config config;
    private SolrClient solrClient;

    CollectorRegistry collectorRegistry = new CollectorRegistry();

    private HTTPServer httpServer;
    private SolrCollector solrCollector;

    /**
     * Constructor.
     *
     * @param port the port number to start server on.
     * @param solrClient the solr client.
     * @param configFile  the configuration file path.
     */
    public SolrExporter(int port, SolrClient solrClient, File configFile) throws IOException {
        this(port, solrClient, new Yaml().loadAs(new FileReader(configFile), Config.class));
    }

    /**
     * Constructor.
     *
     * @param port
     * @param solrClient the solr client.
     * @param config
     */
    public SolrExporter(int port, SolrClient solrClient, Config config) {
        super();

        this.port = port;
        this.solrClient = solrClient;
        this.config = config;

    }

    /**
     * Start HTTP server for exporting Solr metrics.
     *
     */
    public void start() throws MalformedObjectNameException, IOException {
        InetSocketAddress socket = new InetSocketAddress(this.port);

        this.solrCollector = new SolrCollector(this.solrClient, this.config);

        this.collectorRegistry.register(this.solrCollector);

        this.httpServer = new HTTPServer(socket, this.collectorRegistry);
    }

    public void stop() throws IOException {
        this.solrClient.close();
        this.httpServer.stop();
        this.collectorRegistry.unregister(this.solrCollector);
    }

    /**
     * Entry point of SolrServer.
     *
     * @param args the command line arguments
     */
    public static void main( String[] args ) {
        try {
            Properties properties = new Properties();
            properties.load(SolrExporter.class.getResourceAsStream("version.properties"));
            VERSION = (String) properties.get("SOLR_EXPORTER.VERSION");
        } catch (Exception e) {
            logger.warn("Read version.properties failed: " + e.toString());
        }

        ArgumentParser parser = ArgumentParsers.newArgumentParser(SolrCollector.class.getSimpleName())
                .description("Prometheus exporter for Apache Solr.").version(VERSION);

        parser.addArgument(ARG_VERSION_FLAGS).dest(ARG_VERSION_DEST)
                .action(Arguments.version()).help(ARG_VERSION_HELP);

        parser.addArgument(ARG_PORT_FLAGS)
                .metavar(ARG_PORT_METAVAR).dest(ARG_PORT_DEST).type(Integer.class)
                .setDefault(ARG_PORT_DEFAULT).help(ARG_PORT_HELP);

        parser.addArgument(ARG_SOLR_URL_FLAGS)
                .metavar(ARG_SOLR_URL_METAVAR).dest(ARG_SOLR_URL_DEST).type(String.class)
                .setDefault(ARG_SOLR_URL_DEFAULT).help(ARG_SOLR_URL_HELP);

        parser.addArgument(ARG_ZK_HOST_FLAGS)
                .metavar(ARG_ZK_HOST_METAVAR).dest(ARG_ZK_HOST_DEST).type(String.class)
                .setDefault(ARG_ZK_HOST_DEFAULT).help(ARG_ZK_HOST_HELP);

        parser.addArgument(ARG_CONFIG_FLAGS)
                .metavar(ARG_CONFIG_METAVAR).dest(ARG_CONFIG_DEST).type(String.class)
                .setDefault(ARG_CONFIG_DEFAULT).help(ARG_CONFIG_HELP);

        try {
            Namespace res = parser.parseArgs(args);

            int port = res.get(ARG_PORT_DEST);

            String solrUrl = res.getString(ARG_SOLR_URL_DEST);
            String zkHost = res.getString(ARG_ZK_HOST_DEST);
            SolrClient solrClient = null;
            if (!solrUrl.equals("")) {
                NoOpResponseParser responseParser = new NoOpResponseParser();
                responseParser.setWriterType("json");

                HttpSolrClient.Builder builder = new HttpSolrClient.Builder();
                builder.withBaseSolrUrl(res.getString(ARG_SOLR_URL_DEST));

                HttpSolrClient httpSolrClient = builder.build();
                httpSolrClient.setParser(responseParser);

                solrClient = httpSolrClient;
            } else if(!zkHost.equals("")) {
                String host = "";
                String znode = "";

                Pattern pattern = Pattern.compile("(?<host>[^/]+)(?<znode>|(?:/.*))$");
                Matcher matcher = pattern.matcher(zkHost);
                if (matcher.matches()) {
                    host = matcher.group("host") != null ? matcher.group("host") : "";
                    znode = matcher.group("znode") != null ? matcher.group("znode") : "";
                }

                NoOpResponseParser responseParser = new NoOpResponseParser();
                responseParser.setWriterType("json");

                CloudSolrClient.Builder builder = new CloudSolrClient.Builder();
                if (host.contains(",")) {
                    List<String> hosts = new ArrayList<>();
                    for (String h : host.split(",")) {
                        if (StringUtils.isNotEmpty(h)) {
                            hosts.add(h.trim());
                        }
                    }
                    builder.withZkHost(hosts);
                } else {
                    builder.withZkHost(host);
                }
                if (znode.equals("")) {
                    builder.withZkChroot("/");
                } else {
                    builder.withZkChroot(znode);
                }

                CloudSolrClient cloudSolrClient = builder.build();
                cloudSolrClient.setParser(responseParser);

                solrClient = cloudSolrClient;
            } else {
                String message = String.format("Both [%s %s] and [%s %s] parameter are optional, but at least one of them must be required.", ARG_SOLR_URL_FLAGS[0], ARG_SOLR_URL_METAVAR, ARG_ZK_HOST_FLAGS[0], ARG_ZK_HOST_METAVAR);
                parser.handleError(new ArgumentParserException(message, parser));
                System.exit(1);
            }

            File configFile = new File(res.getString(ARG_CONFIG_DEST));

            SolrExporter solrExporter = new SolrExporter(port, solrClient, configFile);
            solrExporter.start();
            logger.info("Start server");
        } catch (MalformedObjectNameException | IOException e) {
            logger.error("Start server failed: " + e.toString());
        } catch (ArgumentParserException e) {
            parser.handleError(e);
        }
    }
}
