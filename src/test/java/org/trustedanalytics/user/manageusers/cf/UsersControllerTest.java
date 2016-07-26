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
package org.trustedanalytics.user.manageusers.cf;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.trustedanalytics.user.common.BlacklistEmailValidator;
import org.trustedanalytics.user.common.FormatUserRolesValidator;
import org.trustedanalytics.user.current.UserDetailsFinder;
import org.trustedanalytics.user.invite.config.AccessTokenDetails;
import org.trustedanalytics.user.manageusers.UserRequest;
import org.trustedanalytics.user.manageusers.UsersController;
import org.trustedanalytics.user.manageusers.UsersService;
import org.trustedanalytics.user.model.UserModel;

import java.util.Optional;
import java.util.UUID;

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
    UsersService priviledgedUsersService;
    @Mock
    UserDetailsFinder detailsFinder;
    @Mock
    private Authentication userAuthentication;
    @Mock
    private BlacklistEmailValidator emailValidator;

    private FormatUserRolesValidator formatRolesValidator = new FormatUserRolesValidator();

    @Before
    public void setup() {
        sut = new UsersController(usersService, priviledgedUsersService, detailsFinder, emailValidator, formatRolesValidator);
        AccessTokenDetails details = new AccessTokenDetails(UUID.randomUUID());
        when(userAuthentication.getDetails()).thenReturn(details);
        req = new UserRequest();
    }

    @Test
    public void getOrgUsers_ByNonManager_PriviledgedServiceNotUsed() {
        UUID orgId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        OAuth2Authentication auth = new OAuth2Authentication(null, userAuthentication);

        when(detailsFinder.findUserId(auth)).thenReturn(userId);
        when(usersService.isOrgAdmin(userId, orgId)).thenReturn(false);
        sut.getOrgUsers(orgId.toString(), auth);

        verify(usersService).isOrgAdmin(userId, orgId);
        verify(usersService, times(1)).getOrgUsers(orgId);
        verify(priviledgedUsersService, times(0)).getOrgUsers(orgId);
    }

    @Test
    public void getOrgUsers_ByManager_PriviledgedServiceUsed() {
        UUID orgId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        OAuth2Authentication auth = new OAuth2Authentication(null, userAuthentication);

        when(detailsFinder.findUserId(auth)).thenReturn(userId);
        when(usersService.isOrgAdmin(userId, orgId)).thenReturn(true);
        sut.getOrgUsers(orgId.toString(), auth);

        verify(usersService).isOrgAdmin(userId, orgId);
        verify(usersService, times(0)).getOrgUsers(orgId);
        verify(priviledgedUsersService, times(1)).getOrgUsers(orgId);
    }

    @Test
    public void createOrgUser_ByNonManager_PriviledgedServiceNotUsed() {
        UUID orgId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        OAuth2Authentication auth = new OAuth2Authentication(null, userAuthentication);

        when(detailsFinder.findUserId(auth)).thenReturn(userId);
        when(detailsFinder.findUserName(auth)).thenReturn("admin_test");
        when(usersService.isOrgAdmin(userId, orgId)).thenReturn(false);
        when(usersService.addOrgUser(any(), any(), any())).thenReturn(Optional.<UserModel>empty());
        sut.createOrgUser(req, orgId.toString(), auth);

        verify(usersService).isOrgAdmin(userId, orgId);
        verify(usersService, times(1)).addOrgUser(req, orgId, "admin_test");
        verify(priviledgedUsersService, times(0)).addOrgUser(req, orgId, "admin_test");
    }

    @Test
    public void deleteOrgUser_ByNonManager_PriviledgedServiceNotUsed() {
        UUID orgId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        OAuth2Authentication auth = new OAuth2Authentication(null, userAuthentication);

        when(detailsFinder.findUserId(auth)).thenReturn(userId);
        when(usersService.isOrgAdmin(userId, orgId)).thenReturn(false);
        sut.deleteUserFromOrg(orgId.toString(), userId.toString(), auth);

        verify(usersService).isOrgAdmin(userId, orgId);
        verify(usersService, times(1)).deleteUserFromOrg(userId, orgId);
        verify(priviledgedUsersService, times(0)).deleteUserFromOrg(userId, orgId);
    }

}
