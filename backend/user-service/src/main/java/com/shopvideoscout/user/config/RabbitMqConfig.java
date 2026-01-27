package com.shopvideoscout.user.config;

import com.shopvideoscout.common.mq.MqConstants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for user-service (voice clone publisher side).
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

    // Voice Clone Exchange & Queue declarations
    @Bean
    public DirectExchange voiceCloneExchange() {
        return new DirectExchange(MqConstants.VOICE_CLONE_EXCHANGE);
    }

    @Bean
    public DirectExchange voiceCloneDlx() {
        return new DirectExchange(MqConstants.VOICE_CLONE_DLX);
    }

    @Bean
    public Queue voiceCloneQueue() {
        return QueueBuilder.durable(MqConstants.VOICE_CLONE_QUEUE)
                .withArgument("x-dead-letter-exchange", MqConstants.VOICE_CLONE_DLX)
                .withArgument("x-dead-letter-routing-key", MqConstants.VOICE_CLONE_DL_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue voiceCloneDlq() {
        return QueueBuilder.durable(MqConstants.VOICE_CLONE_DLQ).build();
    }

    @Bean
    public Binding voiceCloneBinding(Queue voiceCloneQueue, DirectExchange voiceCloneExchange) {
        return BindingBuilder.bind(voiceCloneQueue)
                .to(voiceCloneExchange)
                .with(MqConstants.VOICE_CLONE_ROUTING_KEY);
    }

    @Bean
    public Binding voiceCloneDlBinding(Queue voiceCloneDlq, DirectExchange voiceCloneDlx) {
        return BindingBuilder.bind(voiceCloneDlq)
                .to(voiceCloneDlx)
                .with(MqConstants.VOICE_CLONE_DL_ROUTING_KEY);
    }
}
