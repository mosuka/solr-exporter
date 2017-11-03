# solr-exporter

[Prometheus](https://prometheus.io) exporter for [Apache Solr](http://lucene.apache.org/solr/), written in Java.


## Installing solr-exporter

solr-exporter is available from the release page at [https://github.com/mosuka/solr-exporter/releases](https://github.com/mosuka/solr-exporter/releases).  
For all platform, download the `solr-exporter-<VERSION>.zip` file.  
When getting started, all you need to do is extract the solr-exporter distribution archive to a directory of your choosing.  
To keep things simple for now, extract the solr-exporter distribution archive to your local home directory, for instance on Linux, do:

```text
$ cd ~/
$ unzip solr-exporter-0.2.0-bin.zip
$ tree solr-exporter-0.2.0 
solr-exporter-0.2.0
|-- bin
|   |-- solr-exporter
|   `-- solr-exporter.bat
|-- conf
|   `-- config.yml
|-- lib
|   |-- argparse4j-0.7.0.jar
|   |-- commons-io-2.5.jar
|   |-- commons-math3-3.6.1.jar
|   |-- httpclient-4.5.3.jar
|   |-- httpcore-4.4.6.jar
|   |-- httpmime-4.5.3.jar
|   |-- jackson-annotations-2.9.0.jar
|   |-- jackson-core-2.9.1.jar
|   |-- jackson-databind-2.9.1.jar
|   |-- jackson-jq-0.0.8.jar
|   |-- jcl-over-slf4j-1.7.7.jar
|   |-- jcodings-1.0.13.jar
|   |-- joni-2.1.11.jar
|   |-- log4j-1.2.17.jar
|   |-- noggit-0.8.jar
|   |-- simpleclient-0.0.26.jar
|   |-- simpleclient_common-0.0.26.jar
|   |-- simpleclient_httpserver-0.0.26.jar
|   |-- slf4j-api-1.7.25.jar
|   |-- slf4j-log4j12-1.7.25.jar
|   |-- snakeyaml-1.16.jar
|   |-- solr-exporter-0.2.0.jar
|   |-- solr-solrj-7.1.0.jar
|   |-- stax2-api-3.1.4.jar
|   |-- woodstox-core-asl-4.4.1.jar
|   `-- zookeeper-3.4.10.jar
`-- resources
    `-- log4j.properties

4 directories, 30 files
```

Once extracted, you are now ready to run solr-exporter using the instructions provided in the Running solr-exporter section.

## Running solr-exporter

You can start solr-exporter by running `./bin/solr-exporter` from the solr-exporter directory.

```text
$ ./bin/solr-exporter -p 9983 -b http://localhost:8983/solr -c ./conf/config.yml
```

If you are on Windows platform, you can start solr-exporter by running `.\bin\solr-exporter.bat` instead.

```text
> .\bin\solr-exporter.bat -p 9983 -b http://localhost:8983/solr -c .\conf\config.yml
```

You can also connect to Solr in SolrCloud mode like this.

```text
$ ./bin/solr-exporter -p 9983 -z localhost:2181/solr -c ./conf/config.yml
```

## Building from source

If you want to build solr-exporter from source, check-out the source using `git`.
Binaries are created in the `target` directory.

```text
$ cd ~/
$ git clone git@github.com:mosuka/solr-exporter.git
$ cd solr-exporter
$ mvn package
$ ls -l target
total 20080
drwxr-xr-x   4 minoru  staff       136 Nov  3 17:05 appassembler
drwxr-xr-x   2 minoru  staff        68 Nov  3 17:05 archive-tmp
drwxr-xr-x   3 minoru  staff       102 Nov  3 16:57 classes
drwxr-xr-x   3 minoru  staff       102 Nov  1 23:18 generated-sources
drwxr-xr-x   3 minoru  staff       102 Nov  1 23:18 generated-test-sources
drwxr-xr-x   3 minoru  staff       102 Nov  3 17:05 maven-archiver
drwxr-xr-x   3 minoru  staff       102 Nov  1 23:18 maven-status
-rw-r--r--   1 minoru  staff  10253146 Nov  3 17:05 solr-exporter-0.2.0-bin.zip
-rw-r--r--   1 minoru  staff     21351 Nov  3 17:05 solr-exporter-0.2.0.jar
drwxr-xr-x  12 minoru  staff       408 Nov  3 16:42 surefire-reports
drwxr-xr-x   4 minoru  staff       136 Nov  3 16:57 test-classes
```


## Testing

If you modify the source code, make sure to run the test and succeed.

```text
$ mvn test
```


## Configuration

The configuration is in YAML. An example with all possible options:

```yaml
pingConfig:
  queryConfig:
    path: /admin/ping
  jsonQueries:
    - '.status | { name: "solr_ping_status", type: "gauge", help: "See following URL: http://lucene.apache.org/solr/guide/7_0/ping.html", label_names: [], label_values: [], value: (if . == "OK" then 1.0 else 0.0 end) }'

metricsConfig:
  queryConfig:
    path: /admin/metrics
    params:
      - group: 'all'
      - type: 'all'
      - prefix: ''
      - property: ''
  jsonQueries:
    - '.metrics["solr.jetty"]["org.eclipse.jetty.server.handler.DefaultHandler.1xx-responses"] | { name: "solr_metrics_jetty_response_count",     type: "gauge", help: "See following URL: https://lucene.apache.org/solr/guide/7_0/metrics-reporting.html", label_names: ["status"], label_values: ["1xx"], value: .count }'

...

collectionsConfig:
  queryConfig:
    path: /admin/collections
    params:
      - action: 'CLUSTERSTATUS'
  jsonQueries:
    - '.cluster.live_nodes | length | { name: "solr_collections_cluster_status_live_nodes", type: "gauge", help: "See following URL: http://lucene.apache.org/solr/guide/7_0/collections-api.html#clusterstatus", label_names: [], label_values: [], value: . }'

...

queryConfigs:
  - queryConfig:
      collection: collection1
      path: /select
      params:
        - q: "*:*"
        - start: 0
        - rows: 0
        - json.facet: |-
            {
              category: {
                type: terms,
                field: cat
              }
            }
    jsonQueries:
      - '.facets.category.buckets[] | { name: "solr_facets_category", type: "gauge", help: "Category facets", label_names: ["collection", "term"], label_values: ["collection1", .val], value: .count }'

```


| Name | Description |
| --- | --- |
| pingConfig | Scrape Ping response. See following URL: https://lucene.apache.org/solr/guide/7_0/ping.html |
| metricsConfig | Scrape Metrics Reporting response. See following URL: https://lucene.apache.org/solr/guide/7_0/metrics-reporting.html |
| collectionsConfig | Scrape Collections API response. See following URL: https://lucene.apache.org/solr/guide/7_0/collections-api.html |
| queryConfigs | Scrape Search response. See following URL: https://lucene.apache.org/solr/guide/7_0/searching.html |
