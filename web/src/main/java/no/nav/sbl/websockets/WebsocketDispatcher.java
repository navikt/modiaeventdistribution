package no.nav.sbl.websockets;

import no.nav.sbl.domain.Event;
import org.slf4j.Logger;

import javax.websocket.Session;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static org.slf4j.LoggerFactory.getLogger;

public class WebsocketDispatcher {

    private static final Logger LOG = getLogger(WebsocketDispatcher.class);
    private static ConcurrentHashMap<String, List<Session>> table = new ConcurrentHashMap();

    public static void addSubscriber(Session session) {
        List<Session> sessions = getSessionsForUser(session);
        sessions.add(session);
        table.put(getKey(session), sessions);
    }

    private static String getKey(Session session) {
        return session.getUserPrincipal().getName();
    }

    private static List<Session> getSessionsForUser(Session session) {
        String key = getKey(session);
        List<Session> sessions = table.get(key);
        if (sessions == null) {
            sessions = new ArrayList<>();
        }
        return sessions;
    }

    public static void removeSubscriber(Session session) {
        List<Session> sessions = getSessionsForUser(session);
        sessions.forEach(existingSession -> {
            if (existingSession == session) {
                sessions.remove(session);
            }
        });
    }

    public static void sendEventToWebsocketSubscriber(Event event) {
        List<Session> sessions = new ArrayList<>();

        if (event.veilederIdent != null) {
            sessions = table.get(event.veilederIdent);
        }

        if (sessions != null) {
            sessions.forEach(session -> {
                try {
                    session.getAsyncRemote().sendText(event.eventType);
                } catch (Exception e) {
                    LOG.error("Feil ved utsending av event " + e);
                }
            });
        }
    }
}
