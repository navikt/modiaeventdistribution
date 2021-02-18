package no.nav.modiaeventdistribution.infrastructur

import no.nav.common.health.selftest.SelfTestCheck

interface HealthCheckAware {
    fun getHealthCheck(): SelfTestCheck
}
