package com.hazem.worklink.config;

import com.hazem.worklink.security.CustomUserDetailsService;
import com.hazem.worklink.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Prefix for messages TO the client
        config.enableSimpleBroker("/topic");
        // Prefix for messages FROM the client (handled by @MessageMapping)
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Plain WebSocket endpoint — Angular connects with brokerURL: 'ws://host/ws'
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
    }

    /**
     * Intercept STOMP CONNECT frames to authenticate via JWT.
     * Angular sends: connectHeaders: { Authorization: 'Bearer <token>' }
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authHeader = accessor.getFirstNativeHeader("Authorization");
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String token = authHeader.substring(7);
                        try {
                            String username = jwtUtil.extractUsername(token);
                            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                            if (jwtUtil.validateToken(token, userDetails)) {
                                UsernamePasswordAuthenticationToken auth =
                                        new UsernamePasswordAuthenticationToken(
                                                userDetails, null, userDetails.getAuthorities());
                                accessor.setUser(auth);
                                // Store for presence tracking in WebSocketEventListener
                                String role = userDetails.getAuthorities().iterator().next().getAuthority();
                                if (accessor.getSessionAttributes() != null) {
                                    accessor.getSessionAttributes().put("wsEmail", username);
                                    accessor.getSessionAttributes().put("wsRole", role);
                                }
                            }
                        } catch (Exception ignored) {
                            // Invalid token — connection will proceed without a Principal
                        }
                    }
                }
                return message;
            }
        });
    }
}
