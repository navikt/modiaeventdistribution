import no.nav.brukerdialog.security.Constants;
import no.nav.brukerdialog.tools.SecurityConstants;
import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.dialogarena.config.fasit.ServiceUser;
import no.nav.dialogarena.config.fasit.dto.RestService;
import no.nav.testconfig.ApiAppTest;

import static no.nav.sbl.config.ApplicationConfig.EVENTS_API_URL_PROPERTY_NAME;
import static no.nav.sbl.util.EnvironmentUtils.Type.PUBLIC;
import static no.nav.sbl.util.EnvironmentUtils.Type.SECRET;
import static no.nav.sbl.util.EnvironmentUtils.setProperty;

public class MainTest {

    public static final String APPLICATION_NAME = "modiaeventdistribution";

    public static void main(String[] args) {
        RestService restService = FasitUtils.getRestService("events-api", FasitUtils.getDefaultEnvironment());
        setProperty(EVENTS_API_URL_PROPERTY_NAME, restService.getUrl(), PUBLIC);

        ServiceUser srvmodiaeventdistribution = FasitUtils.getServiceUser("srvmodiaeventdistribution", APPLICATION_NAME);

        String issoHost = FasitUtils.getBaseUrl("isso-host");
        String issoJWS = FasitUtils.getBaseUrl("isso-jwks");
        String issoISSUER = FasitUtils.getBaseUrl("isso-issuer");
        String issoIsAlive = FasitUtils.getBaseUrl("isso.isalive", FasitUtils.Zone.FSS);
        ServiceUser isso_rp_user = FasitUtils.getServiceUser("isso-rp-user", APPLICATION_NAME);
        String redirectUrl = FasitUtils.getBaseUrl("veilarblogin.redirect-url", FasitUtils.Zone.FSS);

        setProperty(Constants.ISSO_HOST_URL_PROPERTY_NAME, issoHost, PUBLIC);
        setProperty(Constants.ISSO_RP_USER_USERNAME_PROPERTY_NAME, isso_rp_user.getUsername(), PUBLIC);
        setProperty(Constants.ISSO_RP_USER_PASSWORD_PROPERTY_NAME, isso_rp_user.getPassword(), PUBLIC);
        setProperty(Constants.ISSO_JWKS_URL_PROPERTY_NAME, issoJWS, PUBLIC);
        setProperty(Constants.ISSO_ISSUER_URL_PROPERTY_NAME, issoISSUER, PUBLIC);
        setProperty(Constants.ISSO_ISALIVE_URL_PROPERTY_NAME, issoIsAlive, PUBLIC);
        setProperty(SecurityConstants.SYSTEMUSER_USERNAME, srvmodiaeventdistribution.getUsername(), SECRET);
        setProperty(SecurityConstants.SYSTEMUSER_PASSWORD, srvmodiaeventdistribution.getPassword(), SECRET);
        setProperty(Constants.OIDC_REDIRECT_URL_PROPERTY_NAME, redirectUrl, PUBLIC);

        ApiAppTest.setupTestContext(ApiAppTest.Config.builder()
                .applicationName(APPLICATION_NAME)
                .build()
        );
        Main.main(args);
    }

}