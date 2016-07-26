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
package org.trustedanalytics.users.orgs;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.security.core.Authentication;
import org.trustedanalytics.user.current.AuthorizationController;
import org.trustedanalytics.user.current.UserDetailsFinder;
import org.trustedanalytics.user.model.Org;
import org.trustedanalytics.user.model.OrgPermission;
import org.trustedanalytics.user.model.UserRole;
import org.trustedanalytics.user.mocks.OrganizationResourceMock;

import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AuthorizationControllerTest {

    private AuthorizationController sut;
    private UUID userGuid = UUID.randomUUID();
    private UUID adminGuid = UUID.randomUUID();
    private Org existingOrganization = new Org(UUID.randomUUID(), "the-only-org");

    @Mock
    private UserDetailsFinder detailsFinder;

    @Mock
    private Authentication userAuthentication;

    @Mock
    private Authentication adminAuthentication;

    @Mock
    private OrganizationResourceMock organizationResource;

    @Before
    public void setup() {
        when(detailsFinder.findUserId(userAuthentication)).thenReturn(userGuid);
        when(detailsFinder.getRole(userAuthentication)).thenReturn(UserRole.USER);
        when(detailsFinder.findUserId(adminAuthentication)).thenReturn(adminGuid);
        when(detailsFinder.getRole(adminAuthentication)).thenReturn(UserRole.ADMIN);
        when(organizationResource.getOrganizations()).thenReturn(Arrays.asList(existingOrganization));

        sut = new AuthorizationController(detailsFinder, organizationResource);
    }

    @Test
    public void getPermissions_user_returnUserPermissions() {
        String orgsRequestParam = String.format("%s,%s", UUID.randomUUID(), existingOrganization.getGuid());
        Collection<OrgPermission> expectedPermissions =
                Arrays.asList(new OrgPermission(existingOrganization, true, false));

        Collection<OrgPermission> returnedPermissions = sut.getPermissions(orgsRequestParam, userAuthentication);

        assertEquals(returnedPermissions, expectedPermissions);
    }

    @Test
    public void getPermissions_consoleAdmin_returnAdminPermissions() {
        String orgsRequestParam = String.format("%s,%s", UUID.randomUUID(), existingOrganization.getGuid());
        Collection<OrgPermission> expectedPermissions =
                Arrays.asList(new OrgPermission(existingOrganization, true, true));

        Collection<OrgPermission> returnedPermissions = sut.getPermissions(orgsRequestParam, adminAuthentication);

        assertEquals(returnedPermissions, expectedPermissions);
    }

    @Test
    public void getPermissions_orgNotProvided_returnPermissionsForAllOrgs() {
        Collection<OrgPermission> returnedPermissions = sut.getPermissions(null, userAuthentication);
        assertThat(returnedPermissions).containsExactly(new OrgPermission(existingOrganization, true, false));
    }
}
