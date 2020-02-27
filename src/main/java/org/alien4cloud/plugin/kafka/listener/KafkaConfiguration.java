package org.alien4cloud.plugin.kafka.listener;

import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Component
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "kafka-listener")
public class KafkaConfiguration {

    private String bootstrapServers;

    private String inputTopic = "a4c-in";
    private String outputTopic = "a4c-out";

    private int timeout = 1000;

    private Map<String,String> producerProperties = new HashMap<String,String>();
    private Map<String,String> consumerProperties = new HashMap<String,String>();

}
