package com.loganalyzer.core.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "log.exchange";
    
    // Ingestion Queue
    public static final String INGESTION_QUEUE = "log.ingestion.queue";
    public static final String INGESTION_ROUTING_KEY = "rout.log.raw";

    // Anomaly/Dispatch Queue
    public static final String ANOMALY_QUEUE = "log.anomaly.queue";
    public static final String ANOMALY_ROUTING_KEY = "rout.log.anomaly";

    @Bean
    public Queue ingestionQueue() {
        return new Queue(INGESTION_QUEUE, true);
    }

    @Bean
    public Queue anomalyQueue() {
        return new Queue(ANOMALY_QUEUE, true);
    }

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Binding bindingIngestionQueue(Queue ingestionQueue, TopicExchange exchange) {
        return BindingBuilder.bind(ingestionQueue).to(exchange).with(INGESTION_ROUTING_KEY);
    }

    @Bean
    public Binding bindingAnomalyQueue(Queue anomalyQueue, TopicExchange exchange) {
        return BindingBuilder.bind(anomalyQueue).to(exchange).with(ANOMALY_ROUTING_KEY);
    }

    @Bean
    public MessageConverter converter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(converter());
        return rabbitTemplate;
    }
}
