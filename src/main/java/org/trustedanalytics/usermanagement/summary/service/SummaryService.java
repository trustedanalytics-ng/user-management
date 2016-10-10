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

import com.google.common.collect.ImmutableList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.trustedanalytics.usermanagement.orgs.mocks.OrgResourceMock;
import org.trustedanalytics.usermanagement.orgs.model.Org;
import org.trustedanalytics.usermanagement.summary.model.OrganizationSummary;
import org.trustedanalytics.usermanagement.summary.model.PlatformSummary;
import org.trustedanalytics.usermanagement.users.model.User;
import org.trustedanalytics.usermanagement.users.service.UsersService;

import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SummaryService {

    private final UsersService usersService;

    @Autowired
    public SummaryService(UsersService usersService) {
        this.usersService = usersService;
    }

    public OrganizationSummary getOrganizationSummary(String orgGuid) {
        // we assume that we have only one organization
        final Org org = OrgResourceMock.get();
        final Collection<User> users = usersService.getOrgUsers(UUID.fromString(orgGuid));

        final OrganizationSummary summary = new OrganizationSummary();
        summary.setName(org.getName());
        summary.setGuid(org.getGuid().toString());
        summary.setUsers(users);
        return summary;
    }

    public PlatformSummary getPlatformSummary() {
        // we assume that we have only one organization
        final Collection<OrganizationSummary> summaries = ImmutableList.of(OrgResourceMock.get()).stream()
                .map(org -> getOrganizationSummary(org.getGuid().toString()))
                .collect(Collectors.toList());
        return new PlatformSummary(summaries);
    }
}
