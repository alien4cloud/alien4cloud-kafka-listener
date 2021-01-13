package org.alien4cloud.plugin.kafka.listener;

import static alien4cloud.utils.AlienUtils.safe;

import org.alien4cloud.plugin.kafka.listener.actions.*;
import org.alien4cloud.plugin.kafka.listener.model.Action;

import org.apache.commons.lang3.StringUtils;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
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
    Producer<String,String> producer;

    @Inject
    private Runworkflow runworkflow;
    @Inject
    private Pullgit pullgit;
    @Inject
    private Createservice createservice;

    private Map<String, AbstractAction> actions = new HashMap<String, AbstractAction>();

    @PostConstruct
    public void init() {
      try  {
        if (configuration.getBootstrapServers() == null || configuration.getInputTopic() == null ||
            configuration.getOutputTopic() == null) {
            log.error("Kafka Listener is not configured.");
        } else {
            Properties props = new Properties();
            props.put("bootstrap.servers", configuration.getBootstrapServers());
            props.put(ConsumerConfig.GROUP_ID_CONFIG, "a4c-kafka-listener");
            props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                  StringDeserializer.class.getName());
            props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                  StringDeserializer.class.getName());
            props.putAll(configuration.getConsumerProperties());

            consumer = new KafkaConsumer<>(props);
            consumer.subscribe(Collections.singletonList(configuration.getInputTopic()));

            props = new Properties();
            props.put("bootstrap.servers", configuration.getBootstrapServers());
            props.put("client.id", "A4C-kafka_listener-plugin");
            props.putAll(configuration.getProducerProperties());

            producer = new KafkaProducer<String, String>(props, new StringSerializer(), new StringSerializer());
        }
      } catch (Exception e) {
         log.error ("Can not connect to kafka ({})", e.getMessage());
      }

       actions.put ("runworkflow", runworkflow);
       actions.put ("pullgit", pullgit);
       actions.put ("createservice", createservice);
    }

    @Scheduled(fixedDelayString = "${kafka-listener.delay:1000}")
    public void listen() {
      synchronized(this) {
       if (consumer != null) {
           log.trace ("Polling Kafka...");
           try {
              ConsumerRecords<String, String> consumerRecords = consumer.poll(configuration.getTimeout());
              if (consumerRecords.count()==0) {
                 log.trace("Nothing found...");
              } else {
                 consumerRecords.forEach(record -> {
                     log.debug("Consumer Record:=[" + record.value() + "]");
                     processMessage(record.value());
                 });
              }
           } catch (WakeupException we) {
              log.debug ("Got WakeupException");
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
          AbstractAction iaction = null;
          if (StringUtils.isNotBlank(saction)) {
             iaction = actions.get(saction.toLowerCase());
             if (iaction == null) {
                log.error ("Request:" + action.getRequestid() + " - action " + 
                           saction + " not implemented");
                sendError (action);
             } else {
                log.info ("Request:" + action.getRequestid() + " - " + saction + 
                          " at " + action.getDatetime());
                Action response = iaction.process(action);
                if (response != null) {
                   String json = (new ObjectMapper()).writeValueAsString(response);
                   doPublish(action.getRequestid(), json);
                }
             }
          } else {
             log.error ("Request:" + action.getRequestid() + " - No action set");
             sendError (action);
          }
       } catch (Exception e) {
          log.error ("Request:" + action.getRequestid() +
                     " - Error running " + action.getAction() + " : " + e.getMessage());
          sendError (action);
       }
    }

    private void sendError(Action request) {
       try {
          Action response = new Action();
          response.setAction("ack");
          response.setRequestid(request.getRequestid());
          response.setDatetime((new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")).format(new Date()).toString());      
          Map<String,String> parameters = new HashMap<String,String>();
          response.setParameters(parameters);
          parameters.put ("status", "KO");
          String json = (new ObjectMapper()).writeValueAsString(response);
          doPublish(request.getRequestid(), json);
       } catch (Exception e) {
          log.error ("Request:" + request.getRequestid() + " - Cannot send response : " + e.getMessage());
       }
    }

    public void sendResponse(Action response) {
       try {
          String json = (new ObjectMapper()).writeValueAsString(response);
          doPublish(response.getRequestid(), json);
       } catch (Exception e) {
          log.error ("Request:" + response.getRequestid() + " - Cannot send response : " + e.getMessage());
       }
    }

    private void doPublish(String requestid, String json) {
        producer.send(new ProducerRecord<>(configuration.getOutputTopic(),requestid,json));
        log.debug("=> KAFKA[{}] : {}",configuration.getOutputTopic(),json);
    }

    @PreDestroy
    public void term() {
      if (consumer != null) {
           // stop polling...
           consumer.wakeup();
      }
      synchronized(this) {
        if (consumer != null) {
           // commits the offset of record to broker. 
           consumer.commitAsync();
           consumer.unsubscribe();
           consumer.close();
           consumer = null;
        }
      }
      if (producer != null) {
          // Close the kafka producer
          producer.close();
      }
    }
}
