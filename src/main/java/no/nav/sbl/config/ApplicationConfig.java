package no.nav.sbl.config;

import no.nav.apiapp.ApiApplication;
import no.nav.apiapp.config.ApiAppConfigurator;
import no.nav.brukerdialog.security.oidc.SystemUserTokenProvider;
import no.nav.metrics.aspects.TimerAspect;
import no.nav.sbl.services.EventService;
import no.nav.sbl.websockets.WebSocketProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Configuration
@EnableAspectJAutoProxy
@EnableScheduling
@Import({
        HelsesjekkConfig.class
})
public class ApplicationConfig implements ApiApplication.NaisApiApplication {

    public static final String EVENTS_API_URL_PROPERTY_NAME = "EVENTS_API_URL";

    @Bean
    public TimerAspect timerAspect() {
        return new TimerAspect();
    }

    @Bean
    public EventService eventService() {
        return new EventService(
                new SystemUserTokenProvider(),
                getRequiredProperty(EVENTS_API_URL_PROPERTY_NAME) + "/"
        );
    }

    @Override
    public boolean brukSTSHelsesjekk() {
        return false;
    }

    @Override
    public void configure(ApiAppConfigurator apiAppConfigurator) {
        apiAppConfigurator.customizeJettyBuilder(jettyBuilder -> jettyBuilder.websocketEndpoint(WebSocketProvider.class));
    }

}
