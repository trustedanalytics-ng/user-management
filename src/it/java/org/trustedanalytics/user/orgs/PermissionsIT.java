/**
 *  Copyright (c) 2015 Intel Corporation 
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
package org.trustedanalytics.user.orgs;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.trustedanalytics.auth.AuthTokenRetriever;
import org.trustedanalytics.user.Application;
import org.trustedanalytics.user.current.UserDetailsFinder;
import org.trustedanalytics.user.mocks.OrganizationResourceMock;
import org.trustedanalytics.user.model.Org;
import org.trustedanalytics.user.model.OrgPermission;
import org.trustedanalytics.user.model.UserRole;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
@IntegrationTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("in-memory")
public class PermissionsIT {

    @Value("http://localhost:${local.server.port}")
    private String BASE_URL;

    @Value("${oauth.resource}/v2/organizations")
    private String cfOrgsUrl;

    @Value("${oauth.resource}/v2/users")
    private String cfUsersUrl;

    @Autowired
    private String TOKEN;

    @Autowired
    private AuthTokenRetriever tokenRetriever;

    @Autowired
    private UserDetailsFinder detailsFinder;

    private Org expectedOrg;

    @Before
    public void setUp() {
        when(tokenRetriever.getAuthToken(any(Authentication.class))).thenReturn(TOKEN);
        expectedOrg = OrganizationResourceMock.get();
    }

    @Test
    public void permissionsEndpoint_globalRoleUser_returnOneUserPermissionForMockedOrg() {
        when(detailsFinder.getRole(any())).thenReturn(UserRole.USER);
        when(detailsFinder.findUserId(any())).thenReturn(UUID.randomUUID());
        TestRestTemplate testRestTemplate = new TestRestTemplate();

        OrgPermission[] valueReturned =
            testRestTemplate.getForObject(BASE_URL + "/rest/orgs/permissions", OrgPermission[].class);

        assertEquals(1, valueReturned.length);
        assertThat(valueReturned).containsExactly(new OrgPermission(expectedOrg, true, false));
    }

    @Test
    public void permissionsEndpoint_globalRoleAdmin_returnOneAdminPermissionForMockedOrg() {
        when(detailsFinder.getRole(any())).thenReturn(UserRole.ADMIN);
        when(detailsFinder.findUserId(any())).thenReturn(UUID.randomUUID());
        TestRestTemplate testRestTemplate = new TestRestTemplate();

        OrgPermission[] valueReturned =
                testRestTemplate.getForObject(BASE_URL + "/rest/orgs/permissions", OrgPermission[].class);

        assertEquals(1, valueReturned.length);
        assertThat(valueReturned).containsExactly(new OrgPermission(expectedOrg, true, true));
    }
}
