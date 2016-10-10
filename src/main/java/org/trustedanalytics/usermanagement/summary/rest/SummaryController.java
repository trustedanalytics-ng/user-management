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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.trustedanalytics.usermanagement.summary.model.OrganizationSummary;
import org.trustedanalytics.usermanagement.summary.model.PlatformSummary;
import org.trustedanalytics.usermanagement.summary.service.SummaryService;

import java.util.Objects;

@RestController
public class SummaryController {

    private final SummaryService service;

    @Autowired
    public SummaryController(SummaryService service) {
        this.service = Objects.requireNonNull(service, "service");
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
