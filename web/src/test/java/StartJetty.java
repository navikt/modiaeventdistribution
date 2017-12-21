import no.nav.brukerdialog.security.context.CustomizableSubjectHandler;
import no.nav.sbl.dialogarena.common.jetty.Jetty;
import no.nav.sbl.websockets.WebSocketProvider;

import static java.lang.System.getProperty;
import static java.lang.System.setProperty;
import static no.nav.brukerdialog.security.context.CustomizableSubjectHandler.setInternSsoToken;
import static no.nav.brukerdialog.security.context.CustomizableSubjectHandler.setUid;
import static no.nav.brukerdialog.tools.ISSOProvider.getIDToken;
import static no.nav.sbl.dialogarena.common.jetty.Jetty.usingWar;
import static no.nav.sbl.dialogarena.common.jetty.JettyStarterUtils.*;
import static no.nav.sbl.dialogarena.test.SystemProperties.setFrom;

public class StartJetty {
    public static void main(String[] args) throws Exception {
        setFrom("environment.properties");
        setProperty("no.nav.brukerdialog.security.context.subjectHandlerImplementationClass", CustomizableSubjectHandler.class.getName());
        setUid(getProperty("veileder.username"));
        setInternSsoToken(getIDToken());

        /*
        Du kan koble deg til websocketen med
            var ws = new WebSocket('ws://localhost:8391/modiaeventdistribution/websocket');
        når appen har startet.

        Dersom det skal jobbes mye appen kan dette f. eks. legges i en .html fil som legges i test-mappa slik at man
        enkelt kan koble seg opp lokalt for testing
        */

        Jetty jetty = usingWar()
                .at("modiaeventdistribution")
                .port(8391)
                .websocketEndpoint(WebSocketProvider.class)
                .disableAnnotationScanning()
                .overrideWebXml()
                .buildJetty();
        jetty.startAnd(first(waitFor(gotKeypress())).then(jetty.stop));
    }
}
