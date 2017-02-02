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
package org.trustedanalytics.usermanagement.security;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.security.core.Authentication;
import org.trustedanalytics.usermanagement.orgs.model.Org;
import org.trustedanalytics.usermanagement.orgs.service.OrganizationsStorage;
import org.trustedanalytics.usermanagement.security.model.OrgPermission;
import org.trustedanalytics.usermanagement.security.rest.AuthorizationController;
import org.trustedanalytics.usermanagement.security.service.UserDetailsFinder;
import org.trustedanalytics.usermanagement.users.model.UserRole;

import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AuthorizationControllerTest {

    private AuthorizationController sut;
    private String userGuid = "test-user";
    private String adminGuid = "test-admin";
    private Org existingOrganization = new Org("defaultorg", "the-only-org");

    @Mock
    private UserDetailsFinder detailsFinder;

    @Mock
    private Authentication userAuthentication;

    @Mock
    private Authentication adminAuthentication;

    @Mock
    private OrganizationsStorage organizationsStorage;

    @Before
    public void setup() {
        when(detailsFinder.findUserId(userAuthentication)).thenReturn(userGuid);
        when(detailsFinder.findUserRole(userAuthentication)).thenReturn(UserRole.USER);
        when(detailsFinder.findUserId(adminAuthentication)).thenReturn(adminGuid);
        when(detailsFinder.findUserRole(adminAuthentication)).thenReturn(UserRole.ADMIN);
        when(organizationsStorage.getOrganizations()).thenReturn(Arrays.asList(existingOrganization));

        sut = new AuthorizationController(detailsFinder, organizationsStorage);
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
