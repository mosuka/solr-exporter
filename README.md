# solr-exporter

[Prometheus](https://prometheus.io) exporter for [Apache Solr](http://lucene.apache.org/solr/), written in Java.


## Installing solr-exporter

solr-exporter is available from the release page at [https://github.com/mosuka/solr-exporter/releases](https://github.com/mosuka/solr-exporter/releases).  
For all platform, download the `solr-exporter-<VERSION>.zip` file.  
When getting started, all you need to do is extract the solr-exporter distribution archive to a directory of your choosing.  
To keep things simple for now, extract the solr-exporter distribution archive to your local home directory, for instance on Linux, do:

```
$ cd ~/
$ unzip solr-exporter-0.1.0-bin.zip
$ $ tree solr-exporter-0.1.0
  solr-exporter-0.1.0
  |-- bin
  |   |-- solr-exporter
  |   `-- solr-exporter.bat
  |-- conf
  |   `-- collectorConfig.yml
  |-- lib
  |   |-- accessors-smart-1.2.jar
  |   |-- argparse4j-0.7.0.jar
  |   |-- asm-5.1.jar
  |   |-- commons-io-2.5.jar
  |   |-- commons-lang3-3.5.jar
  |   |-- commons-math3-3.4.1.jar
  |   |-- httpclient-4.4.1.jar
  |   |-- httpcore-4.4.1.jar
  |   |-- httpmime-4.4.1.jar
  |   |-- jackson-annotations-2.9.0.jar
  |   |-- jackson-core-2.9.1.jar
  |   |-- jackson-databind-2.9.1.jar
  |   |-- jcl-over-slf4j-1.7.7.jar
  |   |-- json-flattener-0.4.0.jar
  |   |-- json-path-2.4.0.jar
  |   |-- json-smart-2.3.jar
  |   |-- log4j-1.2.17.jar
  |   |-- minimal-json-0.9.4.jar
  |   |-- noggit-0.6.jar
  |   |-- simpleclient-0.0.26.jar
  |   |-- simpleclient_common-0.0.26.jar
  |   |-- simpleclient_httpserver-0.0.26.jar
  |   |-- slf4j-api-1.7.25.jar
  |   |-- slf4j-log4j12-1.7.25.jar
  |   |-- snakeyaml-1.16.jar
  |   |-- solr-exporter-0.1.0.jar
  |   |-- solr-solrj-6.6.1.jar
  |   |-- stax2-api-3.1.4.jar
  |   |-- woodstox-core-asl-4.4.1.jar
  |   `-- zookeeper-3.4.10.jar
  `-- resources
      `-- log4j.properties
  
  4 directories, 34 files
```

Once extracted, you are now ready to run solr-exporter using the instructions provided in the Running solr-exporter section.

## Running solr-exporter

You can start solr-exporter by running `./bin/solr-exporter` from the solr-exporter directory.

```
$ ./bin/solr-exporter -p 9983 -c ./conf/config.yml
```

If you are on Windows platform, you can start solr-exporter by running `.\bin\-.bat` instead.

```
> .\bin\solr-exporter.bat -p 9983 -c .\conf\config.yml
```

## Building from source

If you want to build solr-exporter from source, check-out the source using `git`.
Binaries are created in the `target` directory.

```
$ cd ~/
$ git clone git@github.com:mosuka/solr-exporter.git
$ cd solr-exporter
$ mvn package
$ ls -l target
total 17552
drwxr-xr-x   4 mosuka  staff      136 Sep 25 13:28 appassembler
drwxr-xr-x   2 mosuka  staff       68 Sep 25 13:28 archive-tmp
drwxr-xr-x   3 mosuka  staff      102 Sep 25 13:28 classes
drwxr-xr-x   3 mosuka  staff      102 Sep 25 13:28 generated-sources
drwxr-xr-x   3 mosuka  staff      102 Sep 25 13:28 generated-test-sources
drwxr-xr-x   3 mosuka  staff      102 Sep 25 13:28 maven-archiver
drwxr-xr-x   3 mosuka  staff      102 Sep 25 13:28 maven-status
-rw-r--r--   1 mosuka  staff  8932193 Sep 25 13:29 solr-exporter-0.1.0-bin.zip
-rw-r--r--   1 mosuka  staff    51833 Sep 25 13:28 solr-exporter-0.1.0.jar
drwxr-xr-x  24 mosuka  staff      816 Sep 25 13:28 surefire-reports
drwxr-xr-x   4 mosuka  staff      136 Sep 25 13:28 test-classes
```


## Testing

If you modify the source code, make sure to run the test and succeed.

```
$ mvn test
```


## Configuration

The configuration is in YAML. An example with all possible options:

```
#
# Solr on standalone mode
#
#baseUrl: "http://localhost:8983/solr"


#
# Solr on SolrCloud mode
#
zkHosts:
  - "localhost:2181"
  - "localhost:2182"
  - "localhost:2183"
znode: "/solr"


#
# Collections API OVERSEERSTATUS
# https://lucene.apache.org/solr/guide/6_6/cores-api.html#CollectionsAPI-overseerstatus
#
collectionsAPIOverseerStatus:
  enable: true


#
# Collections API CLUSTERSTATUS
# https://lucene.apache.org/solr/guide/6_6/cores-api.html#CollectionsAPI-clusterstatus
#
collectionsAPIClusterStatus:
  enable: true
#  collections:
#    - collection1
#    - collection2
#    - collection3


#
# Ping
# https://lucene.apache.org/solr/guide/6_6/ping.html
#
ping:
  enable: true
#  cores:
#    - collection1
#    - collection2
#    - collection3


#
# CoreAdmin API STATUS
# https://lucene.apache.org/solr/guide/6_6/coreadmin-api.html#CoreAdminAPI-STATUS
#
coreAdminAPIStatus:
  enable: true
#  cores:
#    - collection1
#    - collection2
#    - collection3


#
# MBean Query Handler Stats
# https://lucene.apache.org/solr/guide/6_6/mbean-request-handler.html
#
mBeanRequestHandler:
  enable: true
#  cores:
#    - collection1
#    - collection2
#    - collection3
#  cat: ADMIN
#  key: /admin/mbeans


#
# Metrics Reporting
# https://lucene.apache.org/solr/guide/6_6/metricsReporting-reporting.html
#
metricsReporting:
  enable: true
#  group:
#    - jvm
#    - node
#  type:
#    - counter
#    - gauge
#  prefix:
#    - ADMIN
#    - QUERY


#
# Facet Requests
#
facet:
  enable: true
  queries:
    - collection: collection1
      path: /select
      params:
        - q: "*:*"
        - facet: "on"
        - facet.field: "cat"
        - facet.field: "manu"
        - facet.queryConfig: "inStock:true"
        - facet.queryConfig: "inStock:false"
        - facet.range: "price"
        - f.price.facet.range.start: "0"
        - f.price.facet.range.end: "3000"
        - f.price.facet.range.gap: "1000"
        - facet.range: "popularity"
        - f.popularity.facet.range.start: "0"
        - f.popularity.facet.range.end: "10"
        - f.popularity.facet.range.gap: "1"
        - facet.interval: "price"
        - f.price.facet.interval.set: "[0,10)"
        - f.price.facet.interval.set: "[10,100)"
        - f.price.facet.interval.set: "(100,*]"
        - facet.interval: "popularity"
        - f.popularity.facet.interval.set: "[0,5)"
        - f.popularity.facet.interval.set: "[5,*]"
```


| Name | Description |
| --- | --- |
| baseUrl | Solr base url. Specify this when connecting to Solr in standalone mode. |
| zkHosts | ZooKeeper ensemble. Specify this when connecting to a Solr cluster in SolrCloud mode. |
| znode | znode on Zookeeper. Specify this when connecting to a Solr cluster in SolrCloud mode. |
| collectionsAPIOverseerStatus.enable | Use Solr's Collections API to collect Overseer status as jsonQueries. It is enabled only when connected to Solr in SolrCloud mode. |
| collectionsAPIClusterStatus.enable | Use Solr's Collections API to collect Cluster status as jsonQueries. It is enabled only when connected to Solr in SolrCloud mode. |
| collectionsAPIClusterStatus.collections | Specify specific collections. If omitted, all collections in the cluster are targeted. |
| ping.enable | Use Solr's Ping to collect status as jsonQueries. |
| ping.cores | Specify specific cores. If omitted, all cores in the solr node are targeted. |
| coreAdminAPIStatus.enable | Use Solr's CoreAdminAPI to collect status as jsonQueries. |
| coreAdminAPIStatus.cores | Specify specific cores. If omitted, all cores in the solr node are targeted. |
| mBeanRequestHandler.enable | Use Solr's MBean Request Handler to collect stats as jsonQueries. |
| mBeanRequestHandler.cores | Specify specific cores. If omitted, all cores in the solr node are targeted. |
| mBeanRequestHandler.cat | Restricts results by category name. |
| mBeanRequestHandler.key | Restricts results by object key. |
| metricsReporting.enable | Use Solr's Metrics Reporting to collect jsonQueries. |
| metricsReporting.groups | The metric group to retrieve. The default is all to retrieve all jsonQueries for all groups. |
| metricsReporting.types | The type of metric to retrieve. The default is all to retrieve all metric types. |
| metricsReporting.prefixes | The first characters of metric name that will filter the jsonQueries. |
| facet.enable | Use Solr's Faceting to collect facet counts as jsonQueries. |
| facet.queries | Specify the facet queries. |
| facet.queries.collection | Specify target collection. |
| facet.queries.path | Specify Solr's path. |
| facet.queries.params | Specify facet parameters. |
