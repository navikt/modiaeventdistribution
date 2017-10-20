import no.nav.brukerdialog.security.context.InternbrukerSubjectHandler;
import no.nav.sbl.dialogarena.common.jetty.Jetty;
import no.nav.sbl.websockets.WebSocketProvider;
import org.apache.geronimo.components.jaspi.AuthConfigFactoryImpl;

import javax.security.auth.message.config.AuthConfigFactory;
import java.security.Security;

import static java.lang.System.setProperty;
import static no.nav.brukerdialog.security.context.InternbrukerSubjectHandler.setServicebruker;
import static no.nav.brukerdialog.security.context.InternbrukerSubjectHandler.setVeilederIdent;
import static no.nav.sbl.dialogarena.common.jetty.Jetty.usingWar;
import static no.nav.sbl.dialogarena.common.jetty.JettyStarterUtils.*;

public class StartJetty {
    public static void main(String[] args) throws Exception {
        setVeilederIdent("Z990572");
        setServicebruker("srvmodiaeventdistribution");
        setProperty("no.nav.modig.core.context.subjectHandlerImplementationClass", InternbrukerSubjectHandler.class.getName());
        setProperty("org.apache.geronimo.jaspic.configurationFile", "src/test/resources/jaspiconf.xml");
        Security.setProperty(AuthConfigFactory.DEFAULT_FACTORY_SECURITY_PROPERTY, AuthConfigFactoryImpl.class.getCanonicalName());

        Jetty jetty = usingWar()
                .at("modiaeventdistribution")
                .port(8391)
                .configureForJaspic()
                .websocketEndpoint(WebSocketProvider.class)
                .disableAnnotationScanning()
                .overrideWebXml()
                .loadProperties("/environment.properties")
                .buildJetty();
        jetty.startAnd(first(waitFor(gotKeypress())).then(jetty.stop));

    }

}
