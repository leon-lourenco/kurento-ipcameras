package me.fullcam.kmscontroller.endpoints;
/*
 * Author: Leonardo Lourenço Gomes
 * Checkout my repo! :) https://github.com/fearchannel
 * Created in 01/03/2020
 */

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public class Handler extends TextWebSocketHandler {

    private final Logger logger = LogManager.getLogger(Handler.class);
    private final Gson gson = new GsonBuilder().create();
    private HandlerOperations ops = new HandlerOperations();

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        JsonObject jsonMessage = gson.fromJson(message.getPayload(), JsonObject.class);
        String sessionId = session.getId();

        logger.debug("Message {} from sessionId", jsonMessage, sessionId);
        try {
            switch (jsonMessage.get("id").getAsString()) {
                case "start":
                    ops.start(session, jsonMessage);
                    break;
                case "stop":
                    ops.stop(sessionId);
                    break;
                case "onIceCandidate":
                    ops.onIceCandidate(sessionId, jsonMessage);
                    break;
                default:
                    ops.sendErrorMessage(session, "Invalid message! ID: " + jsonMessage.get("id").getAsString());
                    break;
            }
        } catch (Throwable t) {
            logger.error("Exception handling message {} in sessionId {}", jsonMessage, sessionId, t);
            ops.sendErrorMessage(session, t.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        logger.info("Sessão {} desconectada!",session.getId());        ops.stop(session.getId());
    }

}
