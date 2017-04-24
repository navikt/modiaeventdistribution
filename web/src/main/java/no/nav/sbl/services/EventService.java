package no.nav.sbl.services;

import no.nav.metrics.aspects.Timed;
import no.nav.sbl.domain.Events;
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
import static no.nav.sbl.websockets.WebSocketProvider.sendEventToWebsocketSubscriber;
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
            WebTarget target = client.target(baseUrl + sistLesteEventId);
            Events events = target.request(MediaType.APPLICATION_JSON).get().readEntity(Events.class);
            events.events.forEach(event -> {
                if (event.id > sistLesteEventId) {
                    sistLesteEventId = event.id;
                }
                sendEventToWebsocketSubscriber(event);
            });
        } catch (Exception e) {
            LOGGER.error("Det skjedde en feil ved henting av eventer fra Contextholderen", e);
        } finally {
            laast = false;
        }
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
