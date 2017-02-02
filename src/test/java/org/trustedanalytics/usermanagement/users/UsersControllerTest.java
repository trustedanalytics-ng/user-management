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
package org.trustedanalytics.usermanagement.users;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.trustedanalytics.usermanagement.common.EntityNotFoundException;
import org.trustedanalytics.usermanagement.orgs.model.Org;
import org.trustedanalytics.usermanagement.orgs.service.OrganizationsStorage;
import org.trustedanalytics.usermanagement.security.AccessTokenDetails;
import org.trustedanalytics.usermanagement.security.service.UserDetailsFinder;
import org.trustedanalytics.usermanagement.users.model.UserRequest;
import org.trustedanalytics.usermanagement.users.model.UserRole;
import org.trustedanalytics.usermanagement.users.model.UserRolesRequest;
import org.trustedanalytics.usermanagement.users.rest.UsersController;
import org.trustedanalytics.usermanagement.users.service.UsersService;

import java.util.Optional;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UsersControllerTest {
    private UsersController sut;
    private UserRequest req;

    @Mock
    private UsersService usersService;

    @Mock
    UsersService privilegedUsersService;

    @Mock
    OrganizationsStorage organizationsStorage;

    @Mock
    UserDetailsFinder detailsFinder;

    @Mock
    private Authentication userAuthentication;

    @Mock
    private BlacklistEmailValidator emailValidator;

    private final String orgId = "defaultorg";
    private final String orgName = "test-org-name";
    private final String userId = "test-user";

    @Before
    public void setup() {
        sut = new UsersController(usersService, privilegedUsersService, organizationsStorage,
                detailsFinder, emailValidator);
        AccessTokenDetails details = new AccessTokenDetails(userId);
        when(userAuthentication.getDetails()).thenReturn(details);
        req = new UserRequest();
    }

    @Test(expected = EntityNotFoundException.class)
    public void getOrgUsers_orgDoesNotExist_throwEntityNotFound() {
        OAuth2Authentication auth = new OAuth2Authentication(null, userAuthentication);
        when(detailsFinder.findUserRole(auth)).thenReturn(UserRole.ADMIN);
        when(organizationsStorage.getOrganization(orgId)).thenReturn(Optional.<Org>empty());

        sut.getOrgUsers(orgId, auth);
    }

    @Test
    public void getOrgUsers_ByNonManager_PrivilegedServiceNotUsed() {
        OAuth2Authentication auth = new OAuth2Authentication(null, userAuthentication);
        when(detailsFinder.findUserRole(auth)).thenReturn(UserRole.USER);
        when(organizationsStorage.getOrganization(orgId)).thenReturn(Optional.of(new Org(orgId, orgName)));

        sut.getOrgUsers(orgId, auth);

        verify(detailsFinder).findUserRole(auth);
        verify(usersService, times(1)).getOrgUsers(orgId);
        verify(privilegedUsersService, times(0)).getOrgUsers(orgId);
    }

    @Test
    public void getOrgUsers_ByManager_PrivilegedServiceUsed() {
        OAuth2Authentication auth = new OAuth2Authentication(null, userAuthentication);
        when(detailsFinder.findUserRole(auth)).thenReturn(UserRole.ADMIN);
        when(organizationsStorage.getOrganization(orgId)).thenReturn(Optional.of(new Org(orgId, orgName)));

        sut.getOrgUsers(orgId, auth);

        verify(detailsFinder).findUserRole(auth);
        verify(usersService, times(0)).getOrgUsers(orgId);
        verify(privilegedUsersService, times(1)).getOrgUsers(orgId);
    }

    @Test(expected = EntityNotFoundException.class)
    public void createOrgUser_orgDoesNotExist_throwEntityNotFound() {
        OAuth2Authentication auth = new OAuth2Authentication(null, userAuthentication);
        when(detailsFinder.findUserRole(auth)).thenReturn(UserRole.ADMIN);
        when(organizationsStorage.getOrganization(orgId)).thenReturn(Optional.<Org>empty());
        UserRequest userRequest = new UserRequest(userId, UserRole.USER);

        sut.createOrgUser(userRequest, orgId, auth);
    }

    @Test
    public void createOrgUser_ByNonManager_PrivilegedServiceNotUsed() {
        OAuth2Authentication auth = new OAuth2Authentication(null, userAuthentication);
        when(detailsFinder.findUserRole(auth)).thenReturn(UserRole.USER);
        when(detailsFinder.findUserId(auth)).thenReturn(userId);
        when(detailsFinder.findUserName(auth)).thenReturn("admin_test");
        when(organizationsStorage.getOrganization(orgId)).thenReturn(Optional.of(new Org(orgId, orgName)));

        sut.createOrgUser(req, orgId, auth);

        verify(detailsFinder).findUserRole(auth);
        verify(usersService, times(1)).addOrgUser(req, orgId, "admin_test");
        verify(privilegedUsersService, times(0)).addOrgUser(req, orgId, "admin_test");
    }

    @Test(expected = EntityNotFoundException.class)
    public void deleteOrgUser_orgDoesNotExist_throwEntityNotFound() {
        OAuth2Authentication auth = new OAuth2Authentication(null, userAuthentication);
        when(detailsFinder.findUserRole(auth)).thenReturn(UserRole.ADMIN);
        when(detailsFinder.findUserId(auth)).thenReturn("user-performing-request-id");
        when(organizationsStorage.getOrganization(orgId)).thenReturn(Optional.<Org>empty());

        sut.deleteUserFromOrg(orgId, userId, auth);
    }

    @Test
    public void deleteOrgUser_ByNonManager_PrivilegedServiceNotUsed() {
        String anotherUserId = "another-user";
        OAuth2Authentication auth = new OAuth2Authentication(null, userAuthentication);
        when(detailsFinder.findUserRole(auth)).thenReturn(UserRole.USER);
        when(detailsFinder.findUserId(auth)).thenReturn(anotherUserId);
        when(organizationsStorage.getOrganization(orgId)).thenReturn(Optional.of(new Org(orgId, orgName)));

        sut.deleteUserFromOrg(orgId, userId, auth);

        verify(detailsFinder).findUserRole(auth);
        verify(usersService, times(1)).deleteUserFromOrg(userId, orgId);
        verify(privilegedUsersService, times(0)).deleteUserFromOrg(userId, orgId);
    }

    @Test(expected = AccessDeniedException.class)
    public void deleteYourself_throwsAccessDenied() {
        OAuth2Authentication auth = new OAuth2Authentication(null, userAuthentication);
        when(detailsFinder.findUserRole(auth)).thenReturn(UserRole.ADMIN);
        when(detailsFinder.findUserId(auth)).thenReturn(userId);
        when(organizationsStorage.getOrganization(orgId)).thenReturn(Optional.of(new Org(orgId, orgName)));

        sut.deleteUserFromOrg(orgId, userId, auth);
    }

    @Test(expected = EntityNotFoundException.class)
    public void updateOrgUser_orgDoesNotExist_throwEntityNotFound() {
        OAuth2Authentication auth = new OAuth2Authentication(null, userAuthentication);
        when(detailsFinder.findUserRole(auth)).thenReturn(UserRole.ADMIN);
        when(detailsFinder.findUserId(auth)).thenReturn("user-performing-request-id");
        when(organizationsStorage.getOrganization(orgId)).thenReturn(Optional.<Org>empty());
        UserRolesRequest userRolesRequest = new UserRolesRequest();
        userRolesRequest.setRole(UserRole.USER);

        sut.updateOrgUserRole(userRolesRequest, orgId, userId, auth);
    }

    @Test(expected = AccessDeniedException.class)
    public void updateYourself_throwsAccessDenied() {
        UserRolesRequest request = new UserRolesRequest();
        request.setRole(UserRole.USER);
        OAuth2Authentication auth = new OAuth2Authentication(null, userAuthentication);
        when(detailsFinder.findUserRole(auth)).thenReturn(UserRole.ADMIN);
        when(detailsFinder.findUserId(auth)).thenReturn(userId);
        when(organizationsStorage.getOrganization(orgId)).thenReturn(Optional.of(new Org(orgId, orgName)));

        sut.updateOrgUserRole(request, orgId, userId, auth);
    }
}
