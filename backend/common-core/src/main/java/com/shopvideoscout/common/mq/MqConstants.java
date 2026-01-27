package com.shopvideoscout.common.mq;

/**
 * RabbitMQ constants shared across services.
 */
public final class MqConstants {

    private MqConstants() {}

    public static final String COMPOSE_EXCHANGE = "compose.exchange";
    public static final String COMPOSE_QUEUE = "compose.queue";
    public static final String COMPOSE_ROUTING_KEY = "task.compose";
    public static final String COMPOSE_DLQ = "compose.dlq";
    public static final String COMPOSE_DLX = "compose.dlx";
    public static final String COMPOSE_DL_ROUTING_KEY = "task.compose.dead";

    // Voice Clone Queue
    public static final String VOICE_CLONE_EXCHANGE = "voice.clone.exchange";
    public static final String VOICE_CLONE_QUEUE = "voice.clone.queue";
    public static final String VOICE_CLONE_ROUTING_KEY = "voice.clone";
    public static final String VOICE_CLONE_DLQ = "voice.clone.dlq";
    public static final String VOICE_CLONE_DLX = "voice.clone.dlx";
    public static final String VOICE_CLONE_DL_ROUTING_KEY = "voice.clone.dead";
}
