# Streaming Algorithm for Submodular Function Maximization (cgq-streaming-greedy)

cgq-streaming-greedy is a web service implementing a greedy
Streaming Algorithm for Submodular Function Maximization called Streaming Greedy.
It consists of the following architecture:


![architecture image](https://raw.githubusercontent.com/cswen17/cgq-streaming-greedy/master/public/images/architecture.png)


In this diagram, we can see that there are 4 minimal components: a web service, a Redis cache, AWS Lambda, and a MySQL database.

----
## Background
see [Wikipedia](https://en.wikipedia.org/wiki/Submodular_set_function)

> In mathematics, a submodular set function (also known as a submodular function) is a set function whose value, informally, has the property that the difference in the incremental value of the function that a single element makes when added to an input set decreases as the size of the input set increases. Submodular functions have a natural diminishing returns property which makes them suitable for many applications, including approximation algorithms, game theory (as functions modeling user preferences) and electrical networks. Recently, submodular functions have also found immense utility in several real world problems in machine learning and artificial intelligence, including automatic summarization, multi-document summarization, feature selection, active learning, sensor placement, image collection summarization and many other domains.

----
## Setup
    # assumes you have installed Redis
    # assumes you have an AWS account with Lambda accessible
    # assumes you have your oracle functions set up in Lambda
    # see below for default oracle functions

    $ redis-server
    $ aws configure # input your credentials
    $ sbt run

----
## Usage
    # This part sets up a Streaming Greedy job configuration
    $ curl http://localhost:9000/packaging
    $ export PACKAGING_UUID=$(!$ | tail -n 1)
    $ curl -X PUT -H 'Content-Type: application/json' \
        -d '{"uuid":"$PACKAGING_UUID",
          "values": {
          "ground_set_base_serial_id":"GraphNodesGround",
          "independence_api_serial_id":"GraphNodesIndependence",
          "marginal_value_serial_id":"GraphLogMarginalValue",
          "iterator_api_serial_id":"GraphVertexLabelIterator",
          "element_companion_serial_id":"VertexSet",
          "redis_host":"localhost",
          "redis_port":6379,
          "lambda_ground_set_name":"ground_set_oracle",
          "lambda_independence_name":"independence_oracle",
          "lambda_marginal_value_name":"log_marginal_value",
          "summary_redis_key_name":
               "S_ground_set_oracle_independence_oracle"
          }
        }' http://localhost:9000/packaging/configure

    # You need a validation token to access the algorithm endpoint
    $ curl http://localhost:9000/packaging/validate?uuid=$PACKAGING_UUID
    $ export VALIDATION_TOKEN=$(!$ | tail -n 1)

    # This part initializes the algorithm endpoint
    $ curl -X POST -H 'Content-Type: application/json' -d '{"token":"$VALIDATION_TOKEN"}' http://localhost:9000/algorithm/initialize
    $ export CACHE_KEY=$(!$ | tail -n 1)

    # Finally we have an example of how to use the algorithm
    curl -X POST -H 'Content-Type: text/plain' -d '{"element":{"vertices":"ABCD"},"cacheKey":"$CACHE_KEY"}' http://localhost:9000/algorithm/stream

    # When we are done streaming the algorithm, we can
    # fetch the results using the /summary endpoint. Voila!
    $ curl http://localhost:9000/algorithm/summary?cacheKey=$CACHE_KEY

----
## Oracle functions
Can be found at [To Do: Link].

Copy and paste each oracle function into AWS Lambda. The service will call them each time you use the ``algorithm/stream`` endpoint

----
## thanks
* [markdown-js](https://github.com/evilstreak/markdown-js)

