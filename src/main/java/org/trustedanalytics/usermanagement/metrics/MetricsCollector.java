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
import org.cloudfoundry.identity.uaa.rest.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.trustedanalytics.uaa.UaaOperations;
import org.trustedanalytics.usermanagement.orgs.service.OrganizationsStorage;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Collection;
import java.util.Optional;

public class MetricsCollector {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetricsCollector.class);

    private final int metricsRefreshDelay;
    private final Gauge counts;
    private final UaaOperations uaaPrivilegedClient;
    private final OrganizationsStorage organizationsStorage;

    private ThreadPoolTaskScheduler scheduler;

    public MetricsCollector(int metricsRefreshDelay, Gauge counts, UaaOperations uaaPrivilegedClient,
                            OrganizationsStorage organizationsStorage) {
        this.metricsRefreshDelay = metricsRefreshDelay;
        this.counts = counts;
        this.uaaPrivilegedClient = uaaPrivilegedClient;
        this.organizationsStorage = organizationsStorage;
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
        collectUsers();
        collectOrganizations();
    }

    private void collectUsers() {
        Optional<Integer> usersCount = Optional.ofNullable(uaaPrivilegedClient.getUsers())
                .map(SearchResults::getResources)
                .map(Collection::size);
        if (usersCount.isPresent()) {
            counts.labels("users").set(usersCount.get());
        } else {
            LOGGER.warn("Unable to set users metric: null response when getting users from UAA");
        }
    }

    private void collectOrganizations() {
        int organizationsCount = organizationsStorage.getOrganizations().size();
        counts.labels("organizations").set(organizationsCount);
    }
}
