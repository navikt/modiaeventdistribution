import no.nav.apiapp.ApiApp;
import no.nav.common.nais.utils.NaisYamlUtils;
import no.nav.sbl.config.ApplicationConfig;
import no.nav.sbl.dialogarena.test.SystemProperties;
import no.nav.testconfig.ApiAppTest;

import java.io.IOException;

public class MainTest {

    public static final String APPLICATION_NAME = "modiaeventdistribution";

    public static void main(String[] args) throws IOException {
        SystemProperties.setFrom(".vault.properties");
        NaisYamlUtils.loadFromYaml(".nais/nais-q0.yml");

        ApiAppTest.setupTestContext(ApiAppTest.Config.builder()
                .applicationName(APPLICATION_NAME)
                .build()
        );
        ApiApp.runApp(ApplicationConfig.class, new String[]{"8081"});
    }
}