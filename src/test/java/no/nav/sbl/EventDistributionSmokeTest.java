package no.nav.sbl;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.dialogarena.config.fasit.LoadBalancerConfig;
import no.nav.dialogarena.config.fasit.TestUser;
import no.nav.dialogarena.config.security.ISSOProvider;
import no.nav.sbl.dialogarena.test.ssl.SSLTestUtils;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.api.WebSocketPolicy;
import org.eclipse.jetty.websocket.common.scopes.SimpleContainerScope;
import org.eclipse.jetty.websocket.jsr356.ClientContainer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.websocket.*;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.rangeClosed;
import static javax.ws.rs.client.Entity.json;
import static no.nav.dialogarena.config.security.ISSOProvider.getPriveligertVeileder;
import static no.nav.sbl.dialogarena.test.junit.Smoketest.SMOKETEST_TAG;
import static no.nav.sbl.rest.RestUtils.withClient;
import static org.assertj.core.api.Assertions.assertThat;
import static sun.awt.OSInfo.OSType.WINDOWS;
import static sun.awt.OSInfo.getOSType;

@Tag(SMOKETEST_TAG)
@Slf4j
public class EventDistributionSmokeTest {

    private static final int NUMBER_OF_CLIENTS = getOSType() == WINDOWS ? 1_000 : 5_000;
    private static final int SLEEP_MILLIS = 10_000;
    private static final String EVENT_TYPE = "NY_AKTIV_BRUKER";
    private static final AtomicInteger CLIENT_ID = new AtomicInteger();

    static {
        SSLTestUtils.disableCertificateChecks();
    }

    private HttpClient httpClient = createClient();
    private ClientContext webSocketContainer = new ClientContext();

    @Test
    public void smoketest() throws Exception {
        TestUser priveligertVeileder = getPriveligertVeileder();
        String defaultEnvironment = FasitUtils.getDefaultEnvironment();
        LoadBalancerConfig loadbalancerConfig = FasitUtils.getLoadbalancerConfig("loadbalancer:modiaeventdistribution", defaultEnvironment);
        URI endpoint = UriBuilder.fromPath(loadbalancerConfig.contextRoots)
                .host(loadbalancerConfig.url)
                .scheme("wss")
                .path("ws")
                .path(priveligertVeileder.username)
                .build();
        String contextHolderEndpoint = "https://app-" + defaultEnvironment + ".adeo.no/modiacontextholder/api/context";

        List<WebsocketTestClient> clients = rangeClosed(0, NUMBER_OF_CLIENTS)
                .parallel()
                .mapToObj((i) -> new WebsocketTestClient(endpoint))
                .collect(toList());

        withClient(client -> {
            Map<String, Object> stringObjectMap = new HashMap<>();
            stringObjectMap.put("verdi", "1234");
            stringObjectMap.put("eventType", EVENT_TYPE);
            Invocation.Builder request = client.target(contextHolderEndpoint).request();
            ISSOProvider.getISSOCookies(priveligertVeileder).forEach(cookie -> request.cookie(cookie.getName(), cookie.getValue()));
            Response response = request.post(json(stringObjectMap));
            return assertThat(response.getStatus()).isEqualTo(204);
        });

        Thread.sleep(SLEEP_MILLIS);

        clients.forEach(c -> {
            assertThat(c.open).describedAs("disconnected: " + c).isTrue();
            assertThat(c.errors).describedAs("has errors: " + c).isEmpty();
            assertThat(c.messages).describedAs("messages: " + c).contains(EVENT_TYPE);
        });
    }

    @SneakyThrows
    private static HttpClient createClient() {
        HttpClient httpClient = new HttpClient(new SslContextFactory(true));
        httpClient.start();
        return httpClient;
    }

    private class ClientContext extends ClientContainer {
        @SneakyThrows
        private ClientContext() {
            super(new SimpleContainerScope(WebSocketPolicy.newClientPolicy()), httpClient);
            start();
        }
    }

    @ClientEndpoint
    public class WebsocketTestClient {

        private final int index = CLIENT_ID.incrementAndGet();
        private final List<String> messages = new ArrayList<>();
        private final List<Throwable> errors = new ArrayList<>();
        private boolean open;

        @SneakyThrows
        public WebsocketTestClient(URI endpointURI) {
            log.info("{} connecting!", this);
            webSocketContainer.connectToServer(this, endpointURI);
        }

        @OnOpen
        public void onOpen(Session session) {
            this.open = true;
        }

        @OnMessage
        public void onMessage(String message) {
            log.info("{} message!", this);
            messages.add(message);
        }

        @OnError
        public void onError(Session session, Throwable throwable) {
            log.error(this + " error!", throwable);
            errors.add(throwable);
        }

        @OnClose
        public void onClose(Session session) {
            open = false;
        }

        @Override
        public String toString() {
            return String.format("client #%s", index);
        }
    }
}
