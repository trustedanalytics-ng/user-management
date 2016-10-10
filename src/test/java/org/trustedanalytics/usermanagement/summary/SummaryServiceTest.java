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
import org.trustedanalytics.usermanagement.orgs.mocks.OrgResourceMock;
import org.trustedanalytics.usermanagement.summary.model.PlatformSummary;
import org.trustedanalytics.usermanagement.summary.service.SummaryService;
import org.trustedanalytics.usermanagement.users.model.User;
import org.trustedanalytics.usermanagement.users.model.UserRole;
import org.trustedanalytics.usermanagement.users.service.UsersService;

import java.util.Collection;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SummaryServiceTest {

    private UsersService usersService;
    private SummaryService summaryService;

    @Before
    public void setUp() {
        usersService = mock(UsersService.class);
        this.summaryService = new SummaryService(usersService);
    }

    @Test
    public void getOrganizationSummaryTest() {
        // given
        Collection<User> users = users();
        UUID guid = OrgResourceMock.get().getGuid();

        // when
        when(usersService.getOrgUsers(guid)).thenReturn(users);

        // then
        assertEquals(summaryService.getOrganizationSummary(guid.toString()).getUsers(), users);
    }

    @Test
    public void getPlatformSummaryTest() {
        // given
        Collection<User> users = users();
        UUID guid = OrgResourceMock.get().getGuid();

        // when
        when(usersService.getOrgUsers(guid)).thenReturn(users);

        // then
        PlatformSummary summary = new PlatformSummary(ImmutableList.of(summaryService.getOrganizationSummary(guid.toString())));
        assertEquals(summaryService.getPlatformSummary(), summary);
    }

    private Collection<User> users() {
        return ImmutableList.<User>builder()
                .add(new User("user", UserRole.USER))
                .add(new User("admin", UserRole.ADMIN))
                .build();
    }
}
