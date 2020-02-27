# Alien4cloud Kafka Listener plugin

Plugins that listens to a Kafka topic and performs requested actions

## Configuration
In alien4cloud main configuration file, set:

- kafka-listener.boostrapServers : Kafka server
- kafka-listener.inputTopic : Kafka topic for requests
- kafka-listener.outputTopic : Kafka topic for responses
- kafka-listener.delay : delay between polls (optional)
- kafka-listener.timeout : poll timeout (optional)
- kafka-listener.producerProperties : Kafka properties for producer (optional)
- kafka-listener.consumerProperties : Kafka properties for consumer (optional)

For example to retry connections to kafka only once a minute, set configuration as follows:


    kafka-listener:
    [...]
        producerProperties:
            reconnect.backoff.ms : 60000
            reconnect.backoff.max.ms : 60000
        consumerProperties:
            reconnect.backoff.ms : 60000
            reconnect.backoff.max.ms : 60000

