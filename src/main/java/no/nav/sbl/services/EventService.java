package no.nav.sbl.services;

import no.nav.brukerdialog.security.oidc.SystemUserTokenProvider;
import no.nav.metrics.aspects.Timed;
import no.nav.sbl.domain.Events;
import no.nav.sbl.websockets.WebSocketProvider;
import org.slf4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static no.nav.metrics.MetricsFactory.createEvent;
import static no.nav.sbl.rest.RestUtils.createClient;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.slf4j.LoggerFactory.getLogger;

public class EventService {

    private static final Logger LOGGER = getLogger(EventService.class);

    private final SystemUserTokenProvider systemUserTokenProvider;
    private final String eventApiBaseUrl;
    private long sistLesteEventId;
    private boolean laast = false;
    private Client client = createClient();


    public EventService(SystemUserTokenProvider systemUserTokenProvider, String eventApiBaseUrl) {
        this.systemUserTokenProvider = systemUserTokenProvider;
        this.eventApiBaseUrl = eventApiBaseUrl;
    }

    public long getSistLesteEventId() {
        return sistLesteEventId;
    }

    @Scheduled(fixedRate = 1000)
    @Timed(name = "hentOgDistribuerHendelser")
    public void hentOgDistribuerHendelser() {
        sendMetrikkEventOmAntallTilkoblinger();

        if (laast) {
            return;
        }
        try {
            laast = true;
            Events events = getNewEvents();
            events.events.forEach(event -> {
                if (event.id > sistLesteEventId) {
                    sistLesteEventId = event.id;
                }
                WebSocketProvider.sendEventToWebsocketSubscriber(event);
            });
        } catch (Exception e) {
            LOGGER.error("Det skjedde en feil ved henting av eventer fra Contextholderen", e);
        } finally {
            laast = false;
        }
    }

    private void sendMetrikkEventOmAntallTilkoblinger() {
        int antallTilkoblinger = WebSocketProvider.getAntallTilkoblinger();
        LOGGER.info("antall: {}", antallTilkoblinger);
        createEvent("websockets.tilkoblinger")
                .addFieldToReport("antall", antallTilkoblinger)
                .report();
    }

    public Events getNewEvents() {
        WebTarget target = client.target(eventApiBaseUrl + sistLesteEventId);
        return target
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + systemUserTokenProvider.getToken())
                .get(Events.class);
    }

    @PreDestroy
    @Scheduled(fixedRate = 60000)
    public void dumpTilDisk() throws IOException {
        FileWriter fileWriter = new FileWriter(new File("sistLesteEventId.txt"), false);
        fileWriter.write(String.valueOf(sistLesteEventId));
        fileWriter.close();
    }

    @PostConstruct
    public void lesFraDisk() {
        try {
            sistLesteEventId = Long.parseLong(readFileToString(new File("sistLesteEventId.txt")));
        } catch (IOException e) {
            sistLesteEventId = 0;
        }
    }
}
