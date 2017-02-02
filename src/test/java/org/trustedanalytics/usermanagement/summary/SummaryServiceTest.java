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
package org.trustedanalytics.usermanagement.summary;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.trustedanalytics.usermanagement.orgs.model.Org;
import org.trustedanalytics.usermanagement.orgs.service.OrganizationsStorage;
import org.trustedanalytics.usermanagement.summary.model.PlatformSummary;
import org.trustedanalytics.usermanagement.summary.service.SummaryService;
import org.trustedanalytics.usermanagement.users.model.User;
import org.trustedanalytics.usermanagement.users.model.UserRole;
import org.trustedanalytics.usermanagement.users.service.UsersService;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SummaryServiceTest {

    private UsersService usersService;
    private SummaryService summaryService;
    private OrganizationsStorage organizationsStorage;

    @Before
    public void setUp() {
        usersService = mock(UsersService.class);
        organizationsStorage = mock(OrganizationsStorage.class);
        summaryService = new SummaryService(usersService, organizationsStorage);
    }

    @Test
    public void getOrganizationSummaryTest() {
        // given
        Org org = org();
        Collection<User> users = users();

        // when
        when(usersService.getOrgUsers(org.getGuid())).thenReturn(users);
        when(organizationsStorage.getOrganization(org.getGuid())).thenReturn(Optional.of(org));

        // then
        assertEquals(summaryService.getOrganizationSummary(org.getGuid()).getUsers(), users);
    }

    @Test
    public void getPlatformSummaryTest() {
        // given
        Org org = org();
        Collection<User> users = users();

        // when
        when(usersService.getOrgUsers(org().getGuid())).thenReturn(users);
        when(organizationsStorage.getOrganization(org.getGuid())).thenReturn(Optional.of(org));
        when(organizationsStorage.getOrganizations()).thenReturn(Collections.singleton(org));

        // then
        PlatformSummary summary = new PlatformSummary(ImmutableList.of(summaryService.getOrganizationSummary(org.getGuid())));
        assertEquals(summaryService.getPlatformSummary(), summary);
    }

    private Org org() {
        return new Org("sample-org-id", "sample-org-name");
    }

    private Collection<User> users() {
        return ImmutableList.<User>builder()
                .add(new User("user", UserRole.USER))
                .add(new User("admin", UserRole.ADMIN))
                .build();
    }
}
