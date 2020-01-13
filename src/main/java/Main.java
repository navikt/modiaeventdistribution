import no.nav.apiapp.ApiApp;
import no.nav.brukerdialog.tools.SecurityConstants;
import no.nav.common.nais.utils.NaisUtils;
import no.nav.sbl.config.ApplicationConfig;
import no.nav.sbl.util.EnvironmentUtils;

import static no.nav.sbl.util.EnvironmentUtils.Type.PUBLIC;
import static no.nav.sbl.util.EnvironmentUtils.Type.SECRET;

public class Main {

    public static void main(String[] args) {
        setupVault();
        ApiApp.runApp(ApplicationConfig.class, args);
    }

    private static void setupVault() {
        NaisUtils.Credentials serviceUser = NaisUtils.getCredentials("service_user");

        EnvironmentUtils.setProperty(EnvironmentUtils.resolveSrvUserPropertyName(), serviceUser.username, PUBLIC);
        EnvironmentUtils.setProperty(EnvironmentUtils.resolverSrvPasswordPropertyName(), serviceUser.password, SECRET);
        EnvironmentUtils.setProperty(SecurityConstants.SYSTEMUSER_USERNAME, serviceUser.username, PUBLIC);
        EnvironmentUtils.setProperty(SecurityConstants.SYSTEMUSER_PASSWORD, serviceUser.password, SECRET);
    }
}
