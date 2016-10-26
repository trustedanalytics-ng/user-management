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
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.trustedanalytics.uaa.UaaOperations;
import org.trustedanalytics.usermanagement.orgs.mocks.OrgResourceMock;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

public class MetricsCollector {

    final private int metricsRefreshDelay;
    final private Gauge counts;
    final private UaaOperations uaaPrivilegedClient;
    private OrgResourceMock orgResourceMock;

    private ThreadPoolTaskScheduler scheduler;

    public MetricsCollector(int metricsRefreshDelay, Gauge counts, UaaOperations uaaPrivilegedClient,
                            OrgResourceMock orgResourceMock) {
        this.metricsRefreshDelay = metricsRefreshDelay;
        this.counts = counts;
        this.uaaPrivilegedClient = uaaPrivilegedClient;
        this.orgResourceMock = orgResourceMock;
    }

    @PostConstruct
    public void init() {
        scheduler = new ThreadPoolTaskScheduler();
        scheduler.setAwaitTerminationSeconds(1);
        scheduler.afterPropertiesSet();
        scheduler.scheduleWithFixedDelay(this::collect, metricsRefreshDelay);
    }

    @PreDestroy
    public void finish() {
        scheduler.shutdown();
    }

    public void collect() {
        counts.labels("users").set(uaaPrivilegedClient.getUsers().getResources().size());
        counts.labels("organizations").set(orgResourceMock.getOrganizations().size());
    }
}
