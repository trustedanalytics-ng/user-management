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
package org.trustedanalytics.usermanagement.summary.health;

import feign.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;
import org.trustedanalytics.uaa.UaaOperations;
import org.trustedanalytics.usermanagement.invitations.securitycode.SecurityCodeService;
import org.trustedanalytics.usermanagement.users.rest.AuthGatewayOperations;

import java.util.Objects;

@Component
public class HealthChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(HealthChecker.class);

    private static final String AUTH_GATEWAY_FAILED = "Health check for Auth Gateway failed";

    private final AuthGatewayOperations authGatewayOperations;

    private final SecurityCodeService securityCodeService;

    private final UaaOperations uaaPrivilegedClient;

    @Autowired
    public HealthChecker(AuthGatewayOperations authGatewayOperations,
                         SecurityCodeService securityCodeService,
                         UaaOperations uaaPrivilegedClient) {
        this.authGatewayOperations = Objects.requireNonNull(authGatewayOperations, "authGatewayOperations");
        this.securityCodeService = Objects.requireNonNull(securityCodeService, "securityCodeService");
        this.uaaPrivilegedClient = Objects.requireNonNull(uaaPrivilegedClient, "service");
    }

    private void checkAuthGatewayHealth(Health.Builder healthBuilder) {
        try {
            Response info = authGatewayOperations.getHealth();
            if (info.status() != 200) {
                LOGGER.error(AUTH_GATEWAY_FAILED,info.toString());
                healthBuilder.down().withDetail("AuthGateway", AUTH_GATEWAY_FAILED);
            }
        } catch (Exception e) {
            LOGGER.error(AUTH_GATEWAY_FAILED, e);
            healthBuilder.down().withDetail("AuthGateway", AUTH_GATEWAY_FAILED);
        }
    }

    private void checkUaaHealth(Health.Builder healthBuilder) {
        try {
            uaaPrivilegedClient.getUaaHealth();
        } catch (Exception e) {
            LOGGER.error("Health check for Uaa failed", e);
            healthBuilder.down().withDetail("Uaa", "Health check for Uaa failed");
        }
    }

    private void checkDataBase(Health.Builder healthBuilder) {
        // check if RedisDB/in-memory is responding
        try {
            securityCodeService.getKeys();
        } catch (Exception e) {
            LOGGER.error("Error connecting to database for storing invitations", e);
            healthBuilder.down().withDetail("DataBase", "Error connecting to database for storing invitations");
        }
    }

    public Health checkHealth(Boolean recursive) {
        Health.Builder healthBuilder = new Health.Builder().up();
        checkDataBase(healthBuilder);
        if (recursive) {
            checkAuthGatewayHealth(healthBuilder);
            checkUaaHealth(healthBuilder);
        }
        return healthBuilder.build();
    }
}
