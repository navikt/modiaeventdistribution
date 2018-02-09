package no.nav.sbl.helsesjekk;

import no.nav.apiapp.selftest.Helsesjekk;
import no.nav.apiapp.selftest.HelsesjekkMetadata;
import no.nav.sbl.services.EventService;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import static java.lang.System.getProperty;

@Component
public class EventServiceHelsesjekk implements Helsesjekk {
    private EventService eventService;

    @Inject
    public EventServiceHelsesjekk(EventService eventService) {
        this.eventService = eventService;
    }

    @Override
    public void helsesjekk() throws Throwable {
        eventService.getEventsAfterId(eventService.getSistLesteEventId());
    }

    @Override
    public HelsesjekkMetadata getMetadata() {
        String beskrivelse = String.format("Hente ut siste eventer fra contextholder (siste  leste eventId: %s)", eventService.getSistLesteEventId());

        return new HelsesjekkMetadata(
                "eventservice",
                getProperty("events-api.url"),
                beskrivelse,
                true
        );
    }
}
