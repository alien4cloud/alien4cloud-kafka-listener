# Alien4cloud Kafka Listener plugin

Plugins that listens to a Kafka topic and performs requested actions

## Configuration
In alien4cloud main configuration file, set:

- kafka-listener.boostrapServers : Kafka server
- kafka-listener.inputTopic : Kafka topic for requests
- kafka-listener.outputTopic : Kafka topic for responses
- kafka-listener.delay : delay between polls (optional)
- kafka-listener.timeout : poll timeout (optional)
