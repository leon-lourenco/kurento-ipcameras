package me.fullcam.kmscontroller.endpoints;
/*
 * Author: Leonardo Lourenço Gomes
 * Checkout my repo! :) https://github.com/fearchannel
 * Created in 01/03/2020
 */

import org.kurento.client.KurentoClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@EnableWebSocket
@SpringBootApplication
public class Application implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler(), "/player").setAllowedOrigins("*");
    }

    @Bean
    public Handler handler(){
        return new Handler();
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
