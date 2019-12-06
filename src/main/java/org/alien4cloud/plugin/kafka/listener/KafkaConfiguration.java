package org.alien4cloud.plugin.kafka.listener;

import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "kafka-listener")
public class KafkaConfiguration {

    private String bootstrapServers;

    private String topic = "a4c";

    private int timeout = 1000;
}
