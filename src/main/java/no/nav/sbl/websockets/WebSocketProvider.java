package no.nav.sbl.websockets;


import no.nav.apiapp.util.ObjectUtils;
import no.nav.sbl.domain.Event;
import org.slf4j.Logger;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.slf4j.LoggerFactory.getLogger;

@ServerEndpoint("/ws/{ident}")
public class WebSocketProvider {
    private static final Logger LOG = getLogger(WebSocketProvider.class);
    private static Session sisteSession;

    private static final AtomicBoolean initialized = new AtomicBoolean();

    public WebSocketProvider() {
        if (!initialized.getAndSet(true)) {
            Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(WebSocketProvider::pingClients, 3, 3, TimeUnit.MINUTES);
        }
    }

    private static void pingClients() {
        getOpenSessions().forEach(WebSocketProvider::pingClient);
    }

    private static void pingClient(Session session) {
        try {
            session.getAsyncRemote().sendPing(null);
        } catch (Exception e) {
            LOG.error("Feil ved ping av Websocket-forbindelse", e);
        }
    }

    @OnOpen
    public void onOpen(Session session) {
        sisteSession = session;
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        sisteSession = session;
    }

    @OnClose
    public void onClose(Session session) {
        sisteSession = session;
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        LOG.error(throwable.getMessage(), throwable);
    }

    private static Set<Session> getOpenSessions() {
        return sisteSession == null ? Collections.emptySet() : sisteSession.getOpenSessions();
    }

    public static int getAntallTilkoblinger() {
        return getOpenSessions().size();
    }

    private static String getIdentForSession(Session session) {
        return session.getPathParameters().getOrDefault("ident", "ukjent");
    }

    public static void sendEventToWebsocketSubscriber(Event event) {
        getOpenSessions()
                .stream()
                .filter(session -> ObjectUtils.isEqual(event.veilederIdent, getIdentForSession(session)))
                .forEach(session -> session.getAsyncRemote().sendText(event.eventType));
    }
}