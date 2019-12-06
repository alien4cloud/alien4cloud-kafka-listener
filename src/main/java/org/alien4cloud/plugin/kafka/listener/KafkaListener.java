package org.alien4cloud.plugin.kafka.listener;

import static alien4cloud.utils.AlienUtils.safe;

import org.alien4cloud.plugin.kafka.listener.actions.*;
import org.alien4cloud.plugin.kafka.listener.model.Action;

import org.apache.commons.lang3.StringUtils;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Slf4j
@Service("kafka-listener")
@EnableScheduling
public class KafkaListener {

    @Inject
    private KafkaConfiguration configuration;

    Consumer<String,String> consumer;

    @Inject
    private Runworkflow runworkflow;
    @Inject
    private Pullgit pullgit;

    private Map<String, IAction> actions = new HashMap<String, IAction>();

    @PostConstruct
    public void init() {
        if (configuration.getBootstrapServers() == null || configuration.getTopic() == null) {
            log.error("Kafka Listener is not configured.");
        } else {
            Properties props = new Properties();
            props.put("bootstrap.servers", configuration.getBootstrapServers());
            props.put(ConsumerConfig.GROUP_ID_CONFIG, "a4c-kafka-listener");
            props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                  StringDeserializer.class.getName());
            props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                  StringDeserializer.class.getName());

            consumer = new KafkaConsumer<>(props);
            consumer.subscribe(Collections.singletonList(configuration.getTopic()));
        }

        actions.put ("runworkflow", runworkflow);
        actions.put ("pullgit", pullgit);
    }

    @Scheduled(fixedDelayString = "${kafka-listener.delay:1000}")
    public void listen() {
      synchronized(this) {
       if (consumer != null) {
           log.debug ("Polling Kafka...");
           try {
              ConsumerRecords<String, String> consumerRecords = consumer.poll(configuration.getTimeout());
              if (consumerRecords.count()==0) {
                 log.debug("Nothing found...");
              } else {
                 consumerRecords.forEach(record -> {
                     log.info("Consumer Record:=[" + record.value() + "]");
                     processMessage(record.value());
                 });
              }
           } catch (Exception e) {
              log.error (e.getMessage());
           }
       }
      }
    }

    private void processMessage (String message) {
       Action action = null;
       try {
          action = (new ObjectMapper()).readValue(message, Action.class);
       }
       catch (IOException e) {
          log.error ("Error deserializing [" + message + "]: " + e.getMessage());
       }

      try {
          String saction = action.getAction();
          if (StringUtils.isNotBlank(saction)) {
             IAction iaction = actions.get(saction.toLowerCase());
             if (iaction == null) {
                log.error ("Action " + saction + " not implemented");
             } else {
                iaction.process(action);
             }
          } else {
             log.error ("No action set");
          }
       } catch (Exception e) {
          log.error ("Error running " + action.getAction() + " : " + e.getMessage());
       }
    }

    @PreDestroy
    public void term() {
      synchronized(this) {
        if (consumer != null) {
           // commits the offset of record to broker. 
           consumer.commitAsync();
           consumer.unsubscribe();
           consumer.close();
           consumer = null;
        }
      }
    }
}
