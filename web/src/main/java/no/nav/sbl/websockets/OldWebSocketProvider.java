package no.nav.sbl.websockets;


import no.nav.sbl.domain.Event;
import org.slf4j.Logger;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;

import static no.nav.metrics.MetricsFactory.createEvent;
import static org.slf4j.LoggerFactory.getLogger;

@ServerEndpoint("/websocket")
@Deprecated
public class OldWebSocketProvider {
    private static final Logger LOG = getLogger(OldWebSocketProvider.class);
    private static Session sisteSession;

    @OnOpen
    public void onOpen(Session session) {
        sisteSession = session;
        LOG.warn("Ny tilkobling mot deprecated endepunkt, totalt antall aktive sessions mot endepunktet: ", session.getOpenSessions().size());
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

    public static int getAntallTilkoblinger() {
        if (sisteSession == null) {
            return 0;
        }
        return sisteSession.getOpenSessions().size();
    }

    public static Session getSisteSession() {
        return sisteSession;
    }

    public static void sendEventToWebsocketSubscriber(Event event) {
        if (sisteSession == null) {
            return;
        }
        sisteSession.getOpenSessions().stream()
                .filter(session -> event.veilederIdent.equals(session.getUserPrincipal().getName()))
                .forEach(session -> session.getAsyncRemote().sendText(event.eventType));
    }
}