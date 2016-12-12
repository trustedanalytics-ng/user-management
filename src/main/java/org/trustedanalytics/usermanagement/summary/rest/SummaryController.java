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
package org.trustedanalytics.usermanagement.summary.rest;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.trustedanalytics.usermanagement.summary.health.HealthChecker;
import org.trustedanalytics.usermanagement.summary.model.OrganizationSummary;
import org.trustedanalytics.usermanagement.summary.model.PlatformSummary;
import org.trustedanalytics.usermanagement.summary.service.SummaryService;

@RestController
@ControllerAdvice
public class SummaryController {

    private final SummaryService service;

    private final HealthChecker healthChecker;

    @Autowired
    public SummaryController(SummaryService service,
                             HealthChecker healthChecker) {
        this.service = service;
        this.healthChecker = healthChecker;
    }

    @ApiOperation(
            value = "Get application health status")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 500, message = "Internal server error, e.g. error connecting to RedisDB"),
            @ApiResponse(code = 500, message = "Internal server error of dependant application, e.g. auth gateway")
    })
    @RequestMapping(value = "/healthz", method = RequestMethod.GET)
    public ResponseEntity<Health> getHealth(@RequestParam(value = "recursive", defaultValue = "false") Boolean recursive) {
        Health health = healthChecker.checkHealth(recursive);
        if(health.getStatus() != Status.UP) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(health);
        }
        return ResponseEntity.ok(health);
    }

    @RequestMapping(value = "/rest/organizations/summary", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public PlatformSummary getOrganizationsAndUsers() {
        return service.getPlatformSummary();
    }

    @RequestMapping(value = "/rest/organizations/{orgGuid}/summary", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public OrganizationSummary getOrganizationUsers(@PathVariable("orgGuid") String orgGuid) {
        return service.getOrganizationSummary(orgGuid);
    }
}
