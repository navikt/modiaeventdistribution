package no.nav.sbl.selftest;

import no.nav.sbl.dialogarena.common.web.selftest.SelfTestBaseServlet;
import no.nav.sbl.dialogarena.types.Pingable;
import no.nav.sbl.services.EventService;
import org.slf4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.inject.Inject;
import javax.servlet.ServletException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;

import static java.lang.System.getProperty;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.Arrays.asList;
import static no.nav.sbl.dialogarena.types.Pingable.Ping.feilet;
import static no.nav.sbl.dialogarena.types.Pingable.Ping.lyktes;
import static org.slf4j.LoggerFactory.getLogger;

public class SelftestServlet extends SelfTestBaseServlet {
    private static final Logger logger = getLogger(SelftestServlet.class);
    private static final String APPLIKASJONS_NAVN = "modiaeventdistribution";
    private ApplicationContext ctx = null;

    private EventService eventService;

    @Override
    public void init() throws ServletException {
        ctx = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
        eventService = (EventService) ctx.getBean("eventService");
        super.init();
    }

    @Override
    protected String getApplicationName() {
        return APPLIKASJONS_NAVN;
    }

    @Override
    protected Collection<? extends Pingable> getPingables() {
        return asList(
                pingUrl("MODIACONTEXTHOLDER_EVENTS_API", getProperty("modapp.url") + "/modiacontextholder/internal/isAlive"),
                () -> lyktes("SIST LESTE EVENTID: " + eventService.getSistLesteEventId())
        );
    }


    private Pingable pingUrl(final String name, final String url) {
        return () -> {
            HttpURLConnection connection;
            try {
                connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setConnectTimeout(10000);
                if (connection.getResponseCode() == HTTP_OK) {
                    return lyktes(name);
                } else {
                    logger.warn("<<<<<< Could not connect to {} on {}", name, url);
                    return feilet(name, new RuntimeException(connection.getResponseCode() + " " + connection.getResponseMessage()));
                }
            } catch (Exception e) {
                logger.warn("<<<<<< Could not connect to {} on {}", name, url, e);
                return feilet(name, e);
            }
        };
    }
}