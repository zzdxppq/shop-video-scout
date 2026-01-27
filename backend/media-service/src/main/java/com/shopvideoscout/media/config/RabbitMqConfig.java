package com.shopvideoscout.media.config;

import com.shopvideoscout.common.mq.MqConstants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for media-service (consumer side).
 */
@Configuration
public class RabbitMqConfig {

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }

    @Bean
    public DirectExchange composeExchange() {
        return new DirectExchange(MqConstants.COMPOSE_EXCHANGE);
    }

    @Bean
    public DirectExchange composeDlx() {
        return new DirectExchange(MqConstants.COMPOSE_DLX);
    }

    @Bean
    public Queue composeQueue() {
        return QueueBuilder.durable(MqConstants.COMPOSE_QUEUE)
                .withArgument("x-dead-letter-exchange", MqConstants.COMPOSE_DLX)
                .withArgument("x-dead-letter-routing-key", MqConstants.COMPOSE_DL_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue composeDlq() {
        return QueueBuilder.durable(MqConstants.COMPOSE_DLQ).build();
    }

    @Bean
    public Binding composeBinding(Queue composeQueue, DirectExchange composeExchange) {
        return BindingBuilder.bind(composeQueue)
                .to(composeExchange)
                .with(MqConstants.COMPOSE_ROUTING_KEY);
    }

    @Bean
    public Binding composeDlBinding(Queue composeDlq, DirectExchange composeDlx) {
        return BindingBuilder.bind(composeDlq)
                .to(composeDlx)
                .with(MqConstants.COMPOSE_DL_ROUTING_KEY);
    }
}
