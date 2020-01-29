package me.fullcam.kmscontroller.endpoints;
/*
 * Author: Leonardo Lourenço Gomes
 * Checkout my repo! :) https://github.com/fearchannel
 * Created in 01/03/2020
 */

import lombok.Data;
import org.kurento.client.MediaPipeline;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.WebRtcEndpoint;
@Data
public class UserSession {

    private WebRtcEndpoint webRtcEndpoint;
    private MediaPipeline mediaPipeline;
    private PlayerEndpoint playerEndpoint;

    public UserSession(WebRtcEndpoint webRtcEndpoint, MediaPipeline mediaPipeline, PlayerEndpoint playerEndpoint) {
        this.webRtcEndpoint = webRtcEndpoint;
        this.mediaPipeline = mediaPipeline;
        this.playerEndpoint = playerEndpoint;
    }

    public void release() {
        this.playerEndpoint.stop();
        this.mediaPipeline.release();
    }
}
