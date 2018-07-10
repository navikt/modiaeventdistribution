package no.nav.sbl.config;

import no.nav.sbl.helsesjekk.EventServiceHelsesjekk;
import no.nav.sbl.services.EventService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HelsesjekkConfig {
    @Bean
    public EventServiceHelsesjekk eventServiceHelsesjekk(EventService eventService) {
        return new EventServiceHelsesjekk(eventService);
    };
}
