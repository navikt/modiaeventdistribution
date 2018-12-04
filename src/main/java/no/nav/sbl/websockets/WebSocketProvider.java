package no.nav.sbl.websockets;


import io.micrometer.core.instrument.MeterRegistry;
import no.nav.apiapp.util.ObjectUtils;
import no.nav.json.JsonUtils;
import no.nav.metrics.MetricsFactory;
import no.nav.sbl.domain.Event;
import org.slf4j.Logger;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static no.nav.metrics.MetricsFactory.createEvent;
import static org.slf4j.LoggerFactory.getLogger;

@ServerEndpoint("/ws/{ident}")
public class WebSocketProvider {
    private static final Logger LOG = getLogger(WebSocketProvider.class);
    private static final MeterRegistry meterRegistry = MetricsFactory.getMeterRegistry();

    private static Session sisteSession;

    private static final AtomicBoolean initialized = new AtomicBoolean();

    public WebSocketProvider() {
        if (!initialized.getAndSet(true)) {
            ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
            scheduledExecutorService.scheduleWithFixedDelay(WebSocketProvider::pingClients, 3, 3, TimeUnit.MINUTES);
            scheduledExecutorService.scheduleWithFixedDelay(WebSocketProvider::updateMetrics, 10, 30, TimeUnit.SECONDS);
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

    private static void updateMetrics() {
        int antallTilkoblinger = WebSocketProvider.getAntallTilkoblinger();
        LOG.info("antall: {}", antallTilkoblinger);
        meterRegistry.gauge("websocket_clients", antallTilkoblinger);
        createEvent("websockets.tilkoblinger")
                .addFieldToReport("antall", antallTilkoblinger)
                .report();
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

    public static void sendEventToWebsocketSubscribers(String eventAsJson, @SuppressWarnings("unused") String key) {
        Event event = JsonUtils.fromJson(eventAsJson, Event.class);
        getOpenSessions()
                .stream()
                .filter(session -> ObjectUtils.isEqual(event.veilederIdent, getIdentForSession(session)))
                .forEach(session -> session.getAsyncRemote().sendText(event.eventType));
    }

}