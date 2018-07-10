import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.dialogarena.config.fasit.dto.RestService;
import no.nav.sbl.util.EnvironmentUtils;
import no.nav.testconfig.ApiAppTest;

import static no.nav.sbl.services.EventService.EVENTS_API_URL_PROPERTY_NAME;
import static no.nav.sbl.util.EnvironmentUtils.Type.PUBLIC;

public class MainTest {

    public static void main(String[] args) {
        RestService restService = FasitUtils.getRestService("events-api", FasitUtils.getDefaultEnvironment());
        EnvironmentUtils.setProperty(EVENTS_API_URL_PROPERTY_NAME, restService.getUrl(), PUBLIC);
        ApiAppTest.setupTestContext(ApiAppTest.Config.builder()
                .applicationName("modiaeventdistribution")
                .build()
        );
        Main.main(args);
    }

}