package ovaro.plat4m.config;

import java.security.Principal;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.*;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import ovaro.plat4m.security.AuthoritiesConstants;
import tech.jhipster.config.JHipsterProperties;

@Configuration
@EnableWebSocketMessageBroker
public class WebsocketConfiguration implements WebSocketMessageBrokerConfigurer {

    private static final Logger LOG = LoggerFactory.getLogger(WebsocketConfiguration.class);

    public static final String IP_ADDRESS = "IP_ADDRESS";

    private final JHipsterProperties jHipsterProperties;

    public WebsocketConfiguration(JHipsterProperties jHipsterProperties) {
        this.jHipsterProperties = jHipsterProperties;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setUserDestinationPrefix("/secured/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        LOG.info("Registering web socket endpoints");
        String[] allowedOrigins = Optional.ofNullable(jHipsterProperties.getCors().getAllowedOrigins())
            .map(origins -> origins.toArray(new String[0]))
            .orElse(new String[0]);
        registry
            .addEndpoint("/websocket/tracker")
            .setHandshakeHandler(defaultHandshakeHandler())
            .setAllowedOrigins(allowedOrigins)
            .withSockJS()
            .setInterceptors(httpSessionHandshakeInterceptor());

        registry
            .addEndpoint("/websocket/import")
            .setHandshakeHandler(defaultHandshakeHandler())
            .setAllowedOrigins(allowedOrigins)
            .withSockJS()
            .setInterceptors(httpSessionHandshakeInterceptor());
    }

    @Bean
    public HandshakeInterceptor httpSessionHandshakeInterceptor() {
        return new HandshakeInterceptor() {
            @Override
            public boolean beforeHandshake(
                ServerHttpRequest request,
                ServerHttpResponse response,
                WebSocketHandler wsHandler,
                Map<String, Object> attributes
            ) throws Exception {
                if (request instanceof ServletServerHttpRequest) {
                    ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
                    attributes.put(IP_ADDRESS, servletRequest.getRemoteAddress());
                    LOG.info(
                        "WS beforeHandshake uri={}, principal={}, remoteAddress={}",
                        servletRequest.getURI(),
                        servletRequest.getPrincipal() != null ? servletRequest.getPrincipal().getName() : "null",
                        servletRequest.getRemoteAddress()
                    );
                }
                return true;
            }

            @Override
            public void afterHandshake(
                ServerHttpRequest request,
                ServerHttpResponse response,
                WebSocketHandler wsHandler,
                Exception exception
            ) {
                LOG.info(
                    "WS afterHandshake uri={}, principal={}, exception={}",
                    request.getURI(),
                    request.getPrincipal() != null ? request.getPrincipal().getName() : "null",
                    exception != null ? exception.getMessage() : "none"
                );
            }
        };
    }

    private DefaultHandshakeHandler defaultHandshakeHandler() {
        return new DefaultHandshakeHandler() {
            @Override
            protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
                Principal principal = request.getPrincipal();
                if (principal == null) {
                    Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();
                    authorities.add(new SimpleGrantedAuthority(AuthoritiesConstants.ANONYMOUS));
                    principal = new AnonymousAuthenticationToken("WebsocketConfiguration", "anonymous", authorities);
                }
                LOG.info("WS determineUser uri={}, principal={}", request.getURI(), principal.getName());
                return principal;
            }
        };
    }
}
