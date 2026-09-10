package com.hospital.config;

<<<<<<< HEAD
import org.springframework.beans.factory.annotation.Value;
=======
>>>>>>> origin/main
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

<<<<<<< HEAD
import java.util.Arrays;

=======
>>>>>>> origin/main
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

<<<<<<< HEAD
    @Value("${app.cors.allowed-origins:http://localhost:3000,https://hospitalmanagemen.health}")
    private String allowedOrigins;

=======
>>>>>>> origin/main
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
<<<<<<< HEAD
        String[] origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);

        registry.addEndpoint("/ws-hms")
                .setAllowedOriginPatterns(origins)
=======
        registry.addEndpoint("/ws-hms")
                .setAllowedOrigins("http://localhost:3000")
>>>>>>> origin/main
                .withSockJS();
    }
}
