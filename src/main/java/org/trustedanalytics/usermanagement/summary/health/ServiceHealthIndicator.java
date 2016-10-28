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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.trustedanalytics.usermanagement.users.rest.AuthGatewayOperations;

@Component
public class ServiceHealthIndicator implements HealthIndicator {

  @Autowired
  private AuthGatewayOperations authGatewayOperations;

  @Override
  public Health health() {
    Response info = authGatewayOperations.getHealth();
    if (info.status() != 200) {
      return Health.down().withDetail("ErrorCode", info.reason()).build();
    }
    return Health.up().build();
  }
}
