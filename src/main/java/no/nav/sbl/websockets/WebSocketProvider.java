package no.nav.sbl.websockets;


import no.nav.sbl.domain.Event;
import org.slf4j.Logger;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;

import static no.nav.metrics.MetricsFactory.createEvent;
import static org.slf4j.LoggerFactory.getLogger;

@ServerEndpoint("/ws/{ident}")
public class WebSocketProvider {
    private static final Logger LOG = getLogger(WebSocketProvider.class);
    private static Session sisteSession;

    @OnOpen
    public void onOpen(Session session) {
        sisteSession = session;
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        sisteSession = session;
        String respons = "ping!";
        try {
            session.getBasicRemote().sendText(respons);
        } catch (IOException e) {
            LOG.error("Feil ved ping av Websocket-forbindelse", e);
        }
    }

    @OnClose
    public void onClose(Session session) {
        sisteSession = session;
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        LOG.error(throwable.getMessage(), throwable);
    }

    public static Session getSisteSession() {
        return sisteSession;
    }

    public static int getAntallTilkoblinger() {
        if (sisteSession == null) {
            return 0;
        }
        return sisteSession.getOpenSessions().size();
    }

    private static String getIdentForSession(Session session) {
        return session.getPathParameters().getOrDefault("ident", "ukjent");
    }

    public static void sendEventToWebsocketSubscriber(Event event) {
        if (sisteSession == null) {
            return;
        }
        sisteSession.getOpenSessions().stream()
                .filter(session -> event.veilederIdent.equals(getIdentForSession(session)))
                .forEach(session -> session.getAsyncRemote().sendText(event.eventType));
    }
}