/**
 *  Copyright (c) 2016 Intel Corporation 
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.trustedanalytics.usermanagement.metrics;

import io.prometheus.client.Gauge;
import io.prometheus.client.spring.boot.EnablePrometheusEndpoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.trustedanalytics.uaa.UaaOperations;
import org.trustedanalytics.usermanagement.orgs.mocks.OrgResourceMock;

@Configuration
@EnablePrometheusEndpoint
public class MetricsConfiguration {

    @Value("${metrics.delay}")
    private int metricsRefreshDelay;

    @Value("${metrics.name}")
    private String metricsName;

    @Bean
    public Gauge getCounts() {
        return Gauge.build()
                .name(metricsName)
                .labelNames("component")
                .help("Component count for whole TAP instance.")
                .register();
    }

    @Bean
    public MetricsCollector getMetricsCollector(Gauge counts, UaaOperations uaaPrivilegedClient, OrgResourceMock orgResourceMock) {
        return new MetricsCollector(metricsRefreshDelay, counts, uaaPrivilegedClient, orgResourceMock);
    }
}

