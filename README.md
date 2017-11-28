# solr-exporter

[Prometheus](https://prometheus.io) exporter for [Apache Solr](http://lucene.apache.org/solr/), written in Java.

![solr-exporter-diagram](./solr-exporter-diagram.png "solr-exporter-diagram")

## Installing solr-exporter

solr-exporter is available from the release page at [https://github.com/mosuka/solr-exporter/releases](https://github.com/mosuka/solr-exporter/releases).  
For all platform, download the `solr-exporter-<VERSION>-bin.zip` file.  
When getting started, all you need to do is extract the solr-exporter distribution archive to a directory of your choosing.  
To keep things simple for now, extract the solr-exporter distribution archive to your local home directory, for instance on Linux, do:

```text
$ cd ~/
$ unzip solr-exporter-0.3.2-bin.zip
$ cd solr-exporter-0.3.2
```

Once extracted, you are now ready to run solr-exporter using the instructions provided in the Running solr-exporter section.

## Running solr-exporter

You can start solr-exporter by running `./bin/solr-exporter` from the solr-exporter directory.

```text
$ ./bin/solr-exporter -p 9983 -b http://localhost:8983/solr -f ./conf/config.yml
```

If you are on Windows platform, you can start solr-exporter by running `.\bin\solr-exporter.bat` instead.

```text
> .\bin\solr-exporter.bat -p 9983 -b http://localhost:8983/solr -f .\conf\config.yml
```

You can also connect to Solr in SolrCloud mode like this.

```text
$ ./bin/solr-exporter -p 9983 -z localhost:2181/solr -f ./conf/config.yml
```

See command help:

```text
$ ./bin/solr-exporter -h
usage: SolrCollector [-h] [-v] [-p PORT] [-b BASE_URL] [-z ZK_HOST] [-f CONFIG] [-n NUM_THREADS]

Prometheus exporter for Apache Solr.

optional arguments:
  -h, --help             show this help message and exit
  -v, --version          Show version.
  -p PORT, --port PORT   solr-exporter listen port.
  -b BASE_URL, --baseurl BASE_URL
                         Specify Solr base URL when connecting to Solr in standalone mode (for example 'http://localhost:8983/solr').
  -z ZK_HOST, --zkhost ZK_HOST
                         Specify ZooKeeper connection string when connecting to Solr in SolrCloud mode (for example 'localhost:2181/solr').
  -f CONFIG, --config-file CONFIG
                         Configuration file.
  -n NUM_THREADS, --num-thread NUM_THREADS
                         Number of threads.
```

## Building from source

If you want to build solr-exporter from source, check-out the source using `git`.
Binaries are created in the `target` directory.

```text
$ cd ~/
$ git clone git@github.com:mosuka/solr-exporter.git
$ cd solr-exporter
$ mvn package
```


## Testing

If you modify the source code, make sure to run the test and succeed.

```text
$ mvn test
```


## Configuration

The configuration is in YAML. An example with all possible options:

```yaml
ping:
  query:
    path: /admin/ping
  jsonQueries:
    - '.status | { name: "solr_ping_status", type: "gauge", help: "See following URL: http://lucene.apache.org/solr/guide/7_0/ping.html", label_names: [], label_values: [], value: (if . == "OK" then 1.0 else 0.0 end) }'

metrics:
  query:
    path: /admin/metrics
    params:
      - group: 'all'
      - type: 'all'
      - prefix: ''
      - property: ''
  jsonQueries:
    - '.metrics["solr.jetty"]["org.eclipse.jetty.server.handler.DefaultHandler.1xx-responses"] | { name: "solr_metrics_jetty_response_count",     type: "gauge", help: "See following URL: https://lucene.apache.org/solr/guide/7_0/metrics-reporting.html", label_names: ["status"], label_values: ["1xx"], value: .count }'

...

collections:
  query:
    path: /admin/collections
    params:
      - action: 'CLUSTERSTATUS'
  jsonQueries:
    - '.cluster.live_nodes | length | { name: "solr_collections_cluster_status_live_nodes", type: "gauge", help: "See following URL: http://lucene.apache.org/solr/guide/7_0/collections-api.html#clusterstatus", label_names: [], label_values: [], value: . }'

...

queries:
  - query:
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


Name        | Description
----------- | ---
metrics     | Scrape Metrics Reporting response. See following URL: [https://lucene.apache.org/solr/guide/7_0/metrics-reporting.html](https://lucene.apache.org/solr/guide/7_0/metrics-reporting.html).
collections | Scrape Collections API response. See following URL: [https://lucene.apache.org/solr/guide/7_0/collections-api.html](https://lucene.apache.org/solr/guide/7_0/collections-api.html)/
queries     | Scrape Search response. See following URL: [https://lucene.apache.org/solr/guide/7_0/searching.html](https://lucene.apache.org/solr/guide/7_0/searching.html).
query       | Specify the Solr query parameter. It should contains collection or core (optional), path, params.
jsonQueries | Specify the Json queries to parse json response. For more details, see [https://stedolan.github.io/jq/manual/](https://stedolan.github.io/jq/manual/).

Json query has to output JSON in the following format.

```json
{
  "name": "some_metric_name",
  "type": "gauge", 
  "help": "describe metric.",
  "label_names": ["label_name1", "label_name2"],
  "label_values": ["label_value1", "label_value2"],
  "value": 1.0
}
```

Name         | Description
------------ | ---
name         | Metric name. For more details, see [https://prometheus.io/docs/practices/naming/](https://prometheus.io/docs/practices/naming/).
type         | Metric type. For more detauils, see [https://prometheus.io/docs/concepts/metric_types/](https://prometheus.io/docs/concepts/metric_types/).
help         | Metric help. It must be metric description.
label_names  | Metric label names. For more details, see [https://prometheus.io/docs/practices/naming/](https://prometheus.io/docs/practices/naming/).
label_values | Metric label values. For more details, see [https://prometheus.io/docs/practices/naming/](https://prometheus.io/docs/practices/naming/).
value        | Metric value. It must be Double type.
