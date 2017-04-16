package no.nav.sbl.websockets;


import org.slf4j.Logger;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;

import static no.nav.sbl.websockets.WebsocketDispatcher.addSubscriber;
import static no.nav.sbl.websockets.WebsocketDispatcher.removeSubscriber;
import static org.slf4j.LoggerFactory.getLogger;

@ServerEndpoint("/websocket")
public class WebSocketProvider {
    private static final Logger LOG = getLogger(WebSocketProvider.class);

    @OnOpen
    public void onOpen(Session session){
        try {
            addSubscriber(session);
            session.getBasicRemote().sendText("Connection Established");
        } catch (IOException e) {
            LOG.error("Feil ved opprettelse av Websocket-forbindelse", e);
        }
    }

    @OnMessage
    public void onMessage(String message, Session session){
        String respons = "ping!";
        try {
            session.getBasicRemote().sendText(respons);
        } catch (IOException e) {
            LOG.error("Feil ved ping av Websocket-forbindelse", e);
        }
    }

    @OnClose
    public void onClose(Session session){
        removeSubscriber(session);
    }

}