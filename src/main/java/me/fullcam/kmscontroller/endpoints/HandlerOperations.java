package me.fullcam.kmscontroller.endpoints;
/*
 * Author: Leonardo Lourenço Gomes
 * Checkout my repo! :) https://github.com/fearchannel
 * Created in 01/03/2020
 */

import com.google.gson.JsonObject;
import org.kurento.client.*;
import org.kurento.jsonrpc.JsonUtils;
import org.kurento.jsonrpc.client.JsonRpcClient;
import org.kurento.jsonrpc.client.JsonRpcClientNettyWebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

class HandlerOperations {

    private KurentoClient kurentoClient;

    HandlerOperations() {
        JsonRpcClient jsonRpcClient = new JsonRpcClientNettyWebSocket("ws://localhost:8888/kurento");
        kurentoClient = KurentoClient.createFromJsonRpcClient(jsonRpcClient);
    }

    private final Logger logger = LoggerFactory.getLogger(HandlerOperations.class);
    private final ConcurrentHashMap<String, UserSession> mapOfUsers = new ConcurrentHashMap<>();

    void start(final WebSocketSession session, JsonObject message)
    {
        //1: Media Pipeline
        MediaPipeline pipeline = kurentoClient.createMediaPipeline();
        WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(pipeline).build();
        String videoUrl = message.get("video_url").getAsString();
        final PlayerEndpoint playerEndpoint = new PlayerEndpoint.Builder(pipeline, videoUrl).build();

        final UserSession user = new UserSession(webRtcEndpoint, pipeline, playerEndpoint);
        mapOfUsers.put(session.getId(), user);

        playerEndpoint.connect(webRtcEndpoint);

        //2: WebRtcEndpoint ICE candidates
        webRtcEndpoint.addIceCandidateFoundListener(event -> {
            JsonObject response = new JsonObject();
            response.addProperty("id", "iceCandidate");
            response.add("candidate", JsonUtils.toJsonObject(event.getCandidate()));
            try {
                synchronized (session) {
                    session.sendMessage(new TextMessage(response.toString()));
                }
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        });

        String ofertaSdp = message.get("sdpOffer").getAsString();
        String respostaSdp = webRtcEndpoint.processOffer(ofertaSdp);

        JsonObject response = new JsonObject();
        response.addProperty("id", "startResponse");
        response.addProperty("sdpAnswer", respostaSdp);

        webRtcEndpoint.addMediaStateChangedListener(event -> {
            if (event.getNewState() == MediaState.CONNECTED) {
                logger.info("Session {} connected to camera!", session.getId());
            }
        });
        webRtcEndpoint.gatherCandidates();

        //3: PlayEndpoint
        playerEndpoint.addErrorListener(event -> {
            logger.info("ErrorEvent {}: {} {}", event.getErrorCode(), event.getDescription(), event.getType());
            sendPlayEnd(session);
        });

        playerEndpoint.addEndOfStreamListener(event -> {
            logger.info("EndOfStreamEvent: {}", event.getTimestamp());
            sendPlayEnd(session);
        });

        playerEndpoint.play();
        sendMessage(session, response.toString());
    }

    void stop(String sessionId) {
        UserSession user = mapOfUsers.remove(sessionId);
        if (user != null)
            user.release();
    }

    void onIceCandidate(String sessionId, JsonObject message) {
        UserSession user = mapOfUsers.get(sessionId);

        if (user != null) {
            JsonObject jsonCandidate = message.get("candidate").getAsJsonObject();
            IceCandidate candidate = new IceCandidate(
                    jsonCandidate.get("candidate").getAsString(),
                    jsonCandidate.get("sdpMid").getAsString(),
                    jsonCandidate.get("sdpMLineIndex").getAsInt());
            user.getWebRtcEndpoint().addIceCandidate(candidate);
        }
    }

    void sendErrorMessage(WebSocketSession session, String message) {
        if (mapOfUsers.containsKey(session.getId())) {
            JsonObject response = new JsonObject();
            response.addProperty("id", "error");
            response.addProperty("message", message);
            sendMessage(session, response.toString());
        }
    }

    private void sendMessage(WebSocketSession session, String message) {
        try {
            synchronized (session) {
                session.sendMessage(new TextMessage(message));
            }
        } catch (IOException e) {
            logger.error("Exception sending message", e);
        }
    }

    public void sendPlayEnd(WebSocketSession session) {
        if (mapOfUsers.containsKey(session.getId())) {
            JsonObject response = new JsonObject();
            response.addProperty("id", "playEnd");
            sendMessage(session, response.toString());
        }
    }

}
