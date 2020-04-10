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


## Build alternate qualified plugin archive

You can build an alternative zip archive of this plugin by providing a property named `plugin-qualifier` : 

```
mvn -Dplugin-qualifier=alt clean install
```

This will build an archive, named *alien4cloud-kafka-listener-3.0.0-SNAPSHOT-**alt**.zip* where :

- pluginId and bean_name will be suffixed by **-alt**
- configuration will need to be suffixed by **-alt**
- Kafka groupId and clientId will be suffixed by **-alt**

By this way you can use several instances if the same plugin using different configuration.
