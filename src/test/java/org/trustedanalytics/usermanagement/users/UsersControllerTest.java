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
import org.trustedanalytics.usermanagement.security.AccessTokenDetails;
import org.trustedanalytics.usermanagement.security.service.UserDetailsFinder;
import org.trustedanalytics.usermanagement.users.model.User;
import org.trustedanalytics.usermanagement.users.model.UserRequest;
import org.trustedanalytics.usermanagement.users.model.UserRole;
import org.trustedanalytics.usermanagement.users.model.UserRolesRequest;
import org.trustedanalytics.usermanagement.users.rest.UsersController;
import org.trustedanalytics.usermanagement.users.service.UsersService;

import java.util.Optional;

import static org.mockito.Matchers.any;
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
    UserDetailsFinder detailsFinder;
    @Mock
    private Authentication userAuthentication;
    @Mock
    private BlacklistEmailValidator emailValidator;

    private final String orgId = "defaultorg";
    private final String userId = "test-user";

    @Before
    public void setup() {
        sut = new UsersController(usersService, privilegedUsersService, detailsFinder, emailValidator);
        AccessTokenDetails details = new AccessTokenDetails(userId);
        when(userAuthentication.getDetails()).thenReturn(details);
        req = new UserRequest();
    }

    @Test
    public void getOrgUsers_ByNonManager_PriviledgedServiceNotUsed() {
        OAuth2Authentication auth = new OAuth2Authentication(null, userAuthentication);
        when(detailsFinder.findUserRole(auth)).thenReturn(UserRole.USER);

        sut.getOrgUsers(orgId, auth);

        verify(detailsFinder).findUserRole(auth);
        verify(usersService, times(1)).getOrgUsers(orgId);
        verify(privilegedUsersService, times(0)).getOrgUsers(orgId);
    }

    @Test
    public void getOrgUsers_ByManager_PriviledgedServiceUsed() {
        OAuth2Authentication auth = new OAuth2Authentication(null, userAuthentication);
        when(detailsFinder.findUserRole(auth)).thenReturn(UserRole.ADMIN);

        sut.getOrgUsers(orgId, auth);

        verify(detailsFinder).findUserRole(auth);
        verify(usersService, times(0)).getOrgUsers(orgId);
        verify(privilegedUsersService, times(1)).getOrgUsers(orgId);
    }

    @Test
    public void createOrgUser_ByNonManager_PriviledgedServiceNotUsed() {
        OAuth2Authentication auth = new OAuth2Authentication(null, userAuthentication);
        when(detailsFinder.findUserRole(auth)).thenReturn(UserRole.USER);
        when(detailsFinder.findUserId(auth)).thenReturn(userId);
        when(detailsFinder.findUserName(auth)).thenReturn("admin_test");
        when(usersService.addOrgUser(any(), any(), any())).thenReturn(Optional.<User>empty());

        sut.createOrgUser(req, orgId, auth);

        verify(detailsFinder).findUserRole(auth);
        verify(usersService, times(1)).addOrgUser(req, orgId, "admin_test");
        verify(privilegedUsersService, times(0)).addOrgUser(req, orgId, "admin_test");
    }

    @Test
    public void deleteOrgUser_ByNonManager_PriviledgedServiceNotUsed() {
        String anotherUserId = "another-user";
        OAuth2Authentication auth = new OAuth2Authentication(null, userAuthentication);
        when(detailsFinder.findUserRole(auth)).thenReturn(UserRole.USER);
        when(detailsFinder.findUserId(auth)).thenReturn(anotherUserId);

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

        sut.deleteUserFromOrg(orgId, userId, auth);
    }

    @Test(expected = AccessDeniedException.class)
    public void updateYourself_throwsAccessDenied() {
        UserRolesRequest request = new UserRolesRequest();
        request.setRole(UserRole.USER);
        OAuth2Authentication auth = new OAuth2Authentication(null, userAuthentication);
        when(detailsFinder.findUserRole(auth)).thenReturn(UserRole.ADMIN);
        when(detailsFinder.findUserId(auth)).thenReturn(userId);

        sut.updateOrgUserRole(request, orgId, userId, auth);
    }

}
