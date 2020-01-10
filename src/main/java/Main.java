import no.nav.apiapp.ApiApp;
import no.nav.brukerdialog.security.Constants;
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

        NaisUtils.Credentials issoRPUser = NaisUtils.getCredentials("isso-rp-user");
        EnvironmentUtils.setProperty(Constants.ISSO_RP_USER_USERNAME_PROPERTY_NAME, issoRPUser.username, PUBLIC);
        EnvironmentUtils.setProperty(Constants.ISSO_RP_USER_PASSWORD_PROPERTY_NAME, issoRPUser.password, SECRET);
    }
}
