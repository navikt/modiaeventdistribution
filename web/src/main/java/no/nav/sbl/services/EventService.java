package no.nav.sbl.services;

import no.nav.metrics.aspects.Timed;
import no.nav.sbl.domain.Events;
import no.nav.sbl.websockets.OldWebSocketProvider;
import no.nav.sbl.websockets.WebSocketProvider;
import org.slf4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static java.lang.System.getProperty;
import static javax.ws.rs.client.ClientBuilder.newClient;
import static no.nav.metrics.MetricsFactory.createEvent;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.slf4j.LoggerFactory.getLogger;

public class EventService {
    private static final Logger LOGGER = getLogger(EventService.class);
    private long sistLesteEventId;
    private boolean laast = false;
    private Client client = newClient();
    private String baseUrl = getProperty("events-api.url") + "/";

    public long getSistLesteEventId() {
        return sistLesteEventId;
    }

    @Scheduled(fixedRate = 1000)
    @Timed(name = "hentOgDistribuerHendelser")
    public void hentOgDistribuerHendelser() {
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
                sendMetrikkEventOmAntallTilkoblinger();
                OldWebSocketProvider.sendEventToWebsocketSubscriber(event);
                WebSocketProvider.sendEventToWebsocketSubscriber(event);
            });
        } catch (Exception e) {
            LOGGER.error("Det skjedde en feil ved henting av eventer fra Contextholderen", e);
        } finally {
            laast = false;
        }
    }

    private void sendMetrikkEventOmAntallTilkoblinger() {
        int antallTilkoblingGammelWS = OldWebSocketProvider.getAntallTilkoblinger();
        int antallTilkoblingerNyWS = WebSocketProvider.getAntallTilkoblinger();
        int totaltAntall = antallTilkoblingerNyWS + antallTilkoblingGammelWS;

        createEvent("websockets.tilkoblinger")
                .addFieldToReport("antall", totaltAntall)
                .addFieldToReport("antallGammel", antallTilkoblingGammelWS)
                .addFieldToReport("antallNy", antallTilkoblingerNyWS)
                .report();
    }

    public Events getNewEvents() {
        WebTarget target = client.target(baseUrl + sistLesteEventId);
        return target.request(MediaType.APPLICATION_JSON).get().readEntity(Events.class);
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
