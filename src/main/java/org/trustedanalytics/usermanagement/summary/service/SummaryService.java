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
package org.trustedanalytics.usermanagement.summary.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.trustedanalytics.usermanagement.common.EntityNotFoundException;
import org.trustedanalytics.usermanagement.orgs.model.Org;
import org.trustedanalytics.usermanagement.orgs.service.OrganizationsStorage;
import org.trustedanalytics.usermanagement.summary.model.OrganizationSummary;
import org.trustedanalytics.usermanagement.summary.model.PlatformSummary;
import org.trustedanalytics.usermanagement.users.service.UsersService;

import java.util.Collection;
import java.util.stream.Collectors;

@Service
public class SummaryService {

    private final UsersService usersService;
    private final OrganizationsStorage organizationsStorage;

    @Autowired
    public SummaryService(UsersService usersService, OrganizationsStorage organizationsStorage) {
        this.usersService = usersService;
        this.organizationsStorage = organizationsStorage;
    }

    public OrganizationSummary getOrganizationSummary(String orgGuid) {
        final OrganizationSummary summary = new OrganizationSummary();
        final Org org = organizationsStorage.getOrganization(orgGuid).orElseThrow(() ->
                new EntityNotFoundException(String.format("Organization with ID %s not found", orgGuid)));

        summary.setName(org.getName());
        summary.setGuid(org.getGuid());
        summary.setUsers(usersService.getOrgUsers(orgGuid));

        return summary;
    }

    public PlatformSummary getPlatformSummary() {
        final Collection<OrganizationSummary> summaries =
                organizationsStorage
                        .getOrganizations()
                        .stream()
                        .map(Org::getGuid)
                        .map(this::getOrganizationSummary)
                        .collect(Collectors.toList());
        return new PlatformSummary(summaries);
    }
}
