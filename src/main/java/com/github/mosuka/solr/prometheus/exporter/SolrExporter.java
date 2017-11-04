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
import com.github.mosuka.solr.prometheus.collector.config.SolrCollectorConfig;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.HTTPServer;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
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
    private static final Logger logger = LoggerFactory.getLogger(SolrExporter.class);

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
     * -c, --conn
     */
    private static final String[] ARG_CONN_STR_FLAGS = { "-c", "--conn" };
    private static final String ARG_CONN_STR_METAVAR = "CONN_STR";
    private static final String ARG_CONN_STR_DEST = "connStr";
    private static final String ARG_CONN_STR_DEFAULT = "";
    private static final String ARG_CONN_STR_HELP = "Specify Solr base URL when connecting to Solr in standalone mode (for example 'http://localhost:8983/solr'). If connect to Solr in SolrCloud mode, specify ZooKeeper connection string (for example 'localhost:2181/solr').";

    /**
     * -f, --config-file
     */
    private static final String[] ARG_CONFIG_FLAGS = { "-f", "--config-file" };
    private static final String ARG_CONFIG_METAVAR = "CONFIG";
    private static final String ARG_CONFIG_DEST = "configFile";
    private static final String ARG_CONFIG_DEFAULT = "./conf/config.yml";
    private static final String ARG_CONFIG_HELP = "Configuration file.";

    /**
     * -n, --num-thread
     */
    private static final String[] ARG_NUM_THREADS_FLAGS = { "-n", "--num-thread" };
    private static final String ARG_NUM_THREADS_METAVAR = "NUM_THREADS";
    private static final String ARG_NUM_THREADS_DEST = "numThreads";
    private static final Integer ARG_NUM_THREADS_DEFAULT = 1;
    private static final String ARG_NUM_THREADS_HELP = "Number of threads.";

    private int port;
    private SolrClient solrClient;
    private SolrCollectorConfig config;
    private int numThreads;

    CollectorRegistry registry = new CollectorRegistry();

    private HTTPServer httpServer;
    private SolrCollector collector;

    /**
     * Constructor.
     *
     * @param port the port number to start server on.
     * @param solrClient the solr client.
     * @param configFile  the configuration file path.
     * @param numThreads the number of threads.
     */
    public SolrExporter(int port, SolrClient solrClient, File configFile, int numThreads) throws IOException {
        this(port, solrClient, new Yaml().loadAs(new FileReader(configFile), SolrCollectorConfig.class), numThreads);
    }

    /**
     * Constructor.
     *
     * @param port the port number to start server on.
     * @param solrClient the solr connection string.
     * @param config the configuration.
     * @param numThreads the number of threads.
     */
    public SolrExporter(int port, SolrClient solrClient, SolrCollectorConfig config, int numThreads) {
        super();

        this.port = port;
        this.solrClient = solrClient;
        this.config = config;
        this.numThreads = numThreads;
    }

    /**
     * Start HTTP server for exporting Solr metrics.
     *
     */
    public void start() throws MalformedObjectNameException, IOException {
        InetSocketAddress socket = new InetSocketAddress(port);

        this.collector = new SolrCollector(solrClient, config, numThreads);

        this.registry.register(this.collector);

        this.httpServer = new HTTPServer(socket, this.registry);
    }

    /**
     *
     * @throws IOException
     */
    public void stop() throws IOException {
        this.httpServer.stop();
        this.registry.unregister(this.collector);
    }

    /**
     *
     * @param connStr
     * @return
     */
    private static SolrClient createClient(String connStr) {
        SolrClient solrClient;

        Pattern baseUrlPattern = Pattern.compile("^https?:\\/\\/[\\w\\/:%#\\$&\\?\\(\\)~\\.=\\+\\-]+$");
        Pattern zkHostPattern = Pattern.compile("^(?<host>[^\\/]+)(?<chroot>|(?:\\/.*))$");
        Matcher matcher;

        matcher = baseUrlPattern.matcher(connStr);
        if (matcher.matches()) {
            NoOpResponseParser responseParser = new NoOpResponseParser();
            responseParser.setWriterType("json");

            HttpSolrClient.Builder builder = new HttpSolrClient.Builder();
            builder.withBaseSolrUrl(connStr);

            HttpSolrClient httpSolrClient = builder.build();
            httpSolrClient.setParser(responseParser);

            solrClient = httpSolrClient;
        } else {
            String host = "";
            String chroot = "";

            matcher = zkHostPattern.matcher(connStr);
            if (matcher.matches()) {
                host = matcher.group("host") != null ? matcher.group("host") : "";
                chroot = matcher.group("chroot") != null ? matcher.group("chroot") : "";
            }

            NoOpResponseParser responseParser = new NoOpResponseParser();
            responseParser.setWriterType("json");

            CloudSolrClient.Builder builder = new CloudSolrClient.Builder();
            if (host.contains(",")) {
                List<String> hosts = new ArrayList<>();
                for (String h : host.split(",")) {
                    if (h != null && !h.equals("")) {
                        hosts.add(h.trim());
                    }
                }
                builder.withZkHost(hosts);
            } else {
                builder.withZkHost(host);
            }
            if (chroot.equals("")) {
                builder.withZkChroot("/");
            } else {
                builder.withZkChroot(chroot);
            }

            CloudSolrClient cloudSolrClient = builder.build();
            cloudSolrClient.setParser(responseParser);

            solrClient = cloudSolrClient;
        }

        return solrClient;
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

        parser.addArgument(ARG_CONN_STR_FLAGS)
                .metavar(ARG_CONN_STR_METAVAR).dest(ARG_CONN_STR_DEST).type(String.class)
                .setDefault(ARG_CONN_STR_DEFAULT).help(ARG_CONN_STR_HELP);

        parser.addArgument(ARG_CONFIG_FLAGS)
                .metavar(ARG_CONFIG_METAVAR).dest(ARG_CONFIG_DEST).type(String.class)
                .setDefault(ARG_CONFIG_DEFAULT).help(ARG_CONFIG_HELP);

        parser.addArgument(ARG_NUM_THREADS_FLAGS)
                .metavar(ARG_NUM_THREADS_METAVAR).dest(ARG_NUM_THREADS_DEST).type(Integer.class)
                .setDefault(ARG_NUM_THREADS_DEFAULT).help(ARG_NUM_THREADS_HELP);

        try {
            Namespace res = parser.parseArgs(args);

            int port = res.getInt(ARG_PORT_DEST);
            String connStr = res.getString(ARG_CONN_STR_DEST);
            File configFile = new File(res.getString(ARG_CONFIG_DEST));
            int numThreads = res.getInt(ARG_NUM_THREADS_DEST);

            SolrClient solrClient = createClient(connStr);

            SolrExporter solrExporter = new SolrExporter(port, solrClient, configFile, numThreads);
            solrExporter.start();
            logger.info("Start server");
        } catch (MalformedObjectNameException | IOException e) {
            logger.error("Start server failed: " + e.toString());
        } catch (ArgumentParserException e) {
            parser.handleError(e);
        }
    }
}
