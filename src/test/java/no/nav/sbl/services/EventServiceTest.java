package no.nav.sbl.services;


import com.github.tomakehurst.wiremock.junit.WireMockRule;
import no.nav.brukerdialog.security.oidc.SystemUserTokenProvider;
import no.nav.sbl.domain.Event;
import no.nav.sbl.domain.Events;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.core.HttpHeaders;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EventServiceTest {

    private SystemUserTokenProvider systemUserTokenProvider = mock(SystemUserTokenProvider.class);
    private EventService eventService;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(0);

    @Before
    public void setup(){
        when(systemUserTokenProvider.getToken()).thenReturn("1234");
        eventService = new EventService(systemUserTokenProvider, "http://localhost:" + wireMockRule.port() + "/");
    }

    @Test(expected = NotAuthorizedException.class)
    public void fail(){
        givenThat(get(urlEqualTo("/0"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withBody("")));

        eventService.getNewEvents();
    }

    @Test
    public void ok(){
        givenThat(get(urlEqualTo("/0"))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer 1234"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"events\":[{\"id\":42,\"veilederIdent\":\"Z1234\",\"eventType\":\"NY_AKTIV_ENHET\"}]}")));

        Events newEvents = eventService.getNewEvents();
        assertThat(newEvents.events).hasSize(1);
        Event event = newEvents.events.get(0);
        assertThat(event.id).isEqualTo(42);
        assertThat(event.eventType).isEqualTo("NY_AKTIV_ENHET");
        assertThat(event.veilederIdent).isEqualTo("Z1234");
    }

}