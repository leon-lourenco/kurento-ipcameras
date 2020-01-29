const ws = new WebSocket('ws://172.19.1.26:8443/player');
let videoOutput, videoUrl, webRtcPeer, state = null;

const I_CAN_START = 0;
const I_CAN_STOP = 1;
const I_AM_STARTING = 2;

window.onload = () => {
    console.log('Page loaded ...');
    videoUrl = document.getElementById('rtsp');
    videoUrl.value = "rtsp://admin:kgb8y2@cam.ppasaude.com.br:10000/mode=real&idc=18&ids=1";
	videoOutput = document.getElementById('videoOutput');
	setState(I_CAN_START);
}

window.onbeforeunload = () => {
	ws.close();
}

ws.onmessage = message => {
	let parsedMessage = JSON.parse(message.data);
	console.info('Received message: ' + message.data);

	switch (parsedMessage.id) {
        case 'startResponse':
            startResponse(parsedMessage);
            break;
        case 'error':
            if (state == I_AM_STARTING) {
                setState(I_CAN_START);
            }
            onError('Error message from server: ' + parsedMessage.message);
            break;
        case 'iceCandidate':
            webRtcPeer.addIceCandidate(parsedMessage.candidate)
            break;
        default:
            if (state == I_AM_STARTING) {
                setState(I_CAN_START);
            }
            onError('Unrecognized message', parsedMessage);
	}
}

function start() {
    console.log('Starting video call ...')
    
    setState(I_AM_STARTING);
    showSpinner();

	console.log('Creating WebRtcPeer and generating local sdp offer ...');

    let options = {
        remoteVideo: videoOutput,
        onicecandidate : onIceCandidate
    }

    webRtcPeer = kurentoUtils.WebRtcPeer.WebRtcPeerRecvonly(options, function(error) {
        if(error) return onError(error);
        this.generateOffer(onOffer);
    });
}

function onIceCandidate(candidate) {
	   console.log('Local candidate' + JSON.stringify(candidate));

	   var message = {
	      id : 'onIceCandidate',
	      candidate : candidate
	   };
	   sendMessage(message);
}

function onOffer(error, offerSdp) {
    if(error) return onError(error);

    console.info('Invoking SDP offer callback function ' + location.host);
    
	var message = {
		id : 'start',
        sdpOffer : offerSdp,
        video_url: videoUrl.value
	}
	sendMessage(message);
}

function onError(error) {
	console.error(error);
}

function startResponse(message) {
	setState(I_CAN_STOP);
	console.log('SDP answer received from server. Processing ...');
	webRtcPeer.processAnswer(message.sdpAnswer);
}

function stop() {
    hideSpinner();
	console.log('Stopping video call ...');
    setState(I_CAN_START);
	if (webRtcPeer) {
		webRtcPeer.dispose();
		webRtcPeer = null;
		var message = {
			id : 'stop'
		}
		sendMessage(message);
	}
}

function setState(nextState) {
	switch (nextState) {
        case I_CAN_START:
            $('#start').attr('disabled', false);
            $('#start').attr('onclick', 'start()');
            $('#stop').attr('disabled', true);
            $('#stop').removeAttr('onclick');
            break;
        case I_CAN_STOP:
            $('#start').attr('disabled', true);
            $('#stop').attr('disabled', false);
            $('#stop').attr('onclick', 'stop()');
            break;
        case I_AM_STARTING:
            $('#start').attr('disabled', true);
            $('#start').removeAttr('onclick');
            $('#stop').attr('disabled', true);
            $('#stop').removeAttr('onclick');
            break;
        default:
            onError('Unknown state ' + nextState);
            return;
    }
	state = nextState;
}

function sendMessage(message) {
	var jsonMessage = JSON.stringify(message);
	console.log('Sending message: ' + jsonMessage);
	ws.send(jsonMessage);
}

function showSpinner() {
	videoOutput.style.background = 'center black url("./img/spinner.gif") no-repeat';
}

function hideSpinner() {
	videoOutput.style.background = 'black';
}