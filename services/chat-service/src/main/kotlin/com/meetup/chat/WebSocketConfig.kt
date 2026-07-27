package com.meetup.chat

import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

/**
 * Конфигурация STOMP поверх WebSocket: клиенты подключаются к /ws,
 * шлют сообщения на /app/..., подписываются на рассылку /topic/...
 * Встроенный simple-брокер достаточно для одного инстанса;
 * при масштабировании заменяется на внешний relay (RabbitMQ/Redis).
 */
@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig : WebSocketMessageBrokerConfigurer {
    /** Включает in-memory брокер для /topic и префикс /app для входящих сообщений. */
    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        registry.enableSimpleBroker("/topic")
        registry.setApplicationDestinationPrefixes("/app")
    }

    /** Регистрирует точку подключения WebSocket по пути /ws. */
    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*")
    }
}
