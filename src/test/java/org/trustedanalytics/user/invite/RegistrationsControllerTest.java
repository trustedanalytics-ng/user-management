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
package org.trustedanalytics.user.invite;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.trustedanalytics.user.common.EmptyPasswordException;
import org.trustedanalytics.user.common.EntityNotFoundException;
import org.trustedanalytics.user.common.TooShortPasswordException;
import org.trustedanalytics.user.common.UserPasswordValidator;
import org.trustedanalytics.user.invite.access.AccessInvitations;
import org.trustedanalytics.user.invite.access.AccessInvitationsService;
import org.trustedanalytics.user.invite.rest.InvitationModel;
import org.trustedanalytics.user.invite.rest.RegistrationModel;
import org.trustedanalytics.user.invite.rest.RegistrationsController;
import org.trustedanalytics.user.invite.securitycode.InvalidSecurityCodeException;
import org.trustedanalytics.user.invite.securitycode.SecurityCode;
import org.trustedanalytics.user.invite.securitycode.SecurityCodeService;
import org.trustedanalytics.user.manageusers.UsersService;
import org.trustedanalytics.user.model.UserRole;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RegistrationsControllerTest {

    private static final String USER_EMAIL = "email@example.com";
    private static final String SECURITY_CODE = "code";
    private static final UUID CORE_GUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private RegistrationsController sut;

    @Mock
    private SecurityCodeService securityCodeService;

    @Mock
    private InvitationsService invitationsService;

    @Mock
    private UsersService priviledgedUsersService;

    @Mock
    private AccessInvitationsService accessInvitationsService;

    private UserPasswordValidator passwordValidator = new UserPasswordValidator();

    @Before
    public void setUp() throws Exception {
        sut = new RegistrationsController(securityCodeService, invitationsService,
                                            accessInvitationsService, passwordValidator, priviledgedUsersService);
    }

    @Test(expected = InvalidSecurityCodeException.class)
    public void testAddUser_nullCodeGiven_throwInvalidCode() {
        sut.addUser(new RegistrationModel(), null);
    }

    @Test(expected = InvalidSecurityCodeException.class)
    public void testAddUser_emptyCodeGiven_throwInvalidCode() {
        sut.addUser(new RegistrationModel(), "");
    }

    @Test(expected = InvalidSecurityCodeException.class)
    public void testAddUser_securityCodeInvalid_throwInvalidCode() {
        doThrow(new InvalidSecurityCodeException("")).when(securityCodeService).verify(Matchers.anyString());

        sut.addUser(new RegistrationModel(), SECURITY_CODE);
    }

    @Test(expected = TooShortPasswordException.class)
    public void testAddUser_passwordTooShort_throwTooShortPassword() {
        SecurityCode sc = new SecurityCode(USER_EMAIL, SECURITY_CODE);
        doReturn(sc).when(securityCodeService).verify(Matchers.anyString());
        RegistrationModel registration = new RegistrationModel();
        registration.setPassword("123");

        sut.addUser(registration, SECURITY_CODE);
    }

    @Test(expected = EmptyPasswordException.class)
    public void testAddUser_passwordEmpty_throwEmptyPassword() {
        SecurityCode sc = new SecurityCode(USER_EMAIL, SECURITY_CODE);
        doReturn(sc).when(securityCodeService).verify(Matchers.anyString());
        RegistrationModel registration = new RegistrationModel();
        registration.setPassword("");

        sut.addUser(registration, SECURITY_CODE);
    }

    @Test(expected = UserExistsException.class)
    public void testAddUser_createUserAlreadyExists_throwUserExistsException() {
        SecurityCode sc = new SecurityCode(USER_EMAIL, SECURITY_CODE);
        doReturn(sc).when(securityCodeService).verify(Matchers.anyString());
        RegistrationModel registration = new RegistrationModel();
        registration.setPassword("123456");


        doThrow(new UserExistsException("")).when(invitationsService).createUser(
                Matchers.anyString(), Matchers.anyString());

        sut.addUser(registration, SECURITY_CODE);
    }

    @Test(expected = OrgExistsException.class)
    public void testAddUser_createUserAlreadyExistsNoOrg_throwOrgExistsException() {
        SecurityCode sc = new SecurityCode(USER_EMAIL, SECURITY_CODE);
        doReturn(sc).when(securityCodeService).verify(Matchers.anyString());
        RegistrationModel registration = new RegistrationModel();
        registration.setPassword("123456");
        doThrow(new OrgExistsException("")).when(invitationsService).createUser(
                Matchers.anyString(), Matchers.anyString());

        sut.addUser(registration, SECURITY_CODE);
    }

    @Test(expected = HttpClientErrorException.class)
    public void testAddUser_createUserHttpConnectionError_throwHttpError() {
        SecurityCode sc = new SecurityCode(USER_EMAIL, SECURITY_CODE);
        doReturn(sc).when(securityCodeService).verify(Matchers.anyString());
        RegistrationModel registration = new RegistrationModel();
        registration.setPassword("123456");
        doThrow(new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR)).when(invitationsService).createUser(
                Matchers.anyString(), Matchers.anyString());

        sut.addUser(registration, SECURITY_CODE);
    }

    @Test(expected = InvalidOrganizationNameException.class)
    public void testAddUser_createUserNoOrgHttpConnectionError_throwHttpError() {
        SecurityCode sc = new SecurityCode(USER_EMAIL, SECURITY_CODE);
        doReturn(sc).when(securityCodeService).verify(Matchers.anyString());
        RegistrationModel registration = new RegistrationModel();
        registration.setPassword("123456");
        doThrow(new InvalidOrganizationNameException("")).when(invitationsService).createUser(
                Matchers.anyString(), Matchers.anyString());

        sut.addUser(registration, SECURITY_CODE);
    }

    @Test()
    public void testAddUser_allOk_useCode() {
        SecurityCode sc = new SecurityCode(USER_EMAIL, SECURITY_CODE);
        doReturn(sc).when(securityCodeService).verify(Matchers.anyString());
        RegistrationModel registration = new RegistrationModel();
        String userPassword = "123456";
        UUID userGuid = UUID.randomUUID();
        when(invitationsService.createUser(USER_EMAIL, userPassword)).thenReturn(Optional.of(userGuid));

        AccessInvitations accessInvitations = new AccessInvitations();
        accessInvitations.getOrgAccessInvitations().put(CORE_GUID, UserRole.ADMIN);
        when(accessInvitationsService
                .getAccessInvitations(USER_EMAIL)).thenReturn(Optional.of(accessInvitations));
        registration.setPassword(userPassword);

        RegistrationModel registeredUser = sut.addUser(registration, SECURITY_CODE);

        Mockito.verify(securityCodeService).redeem(sc);
        Assert.assertTrue(registeredUser.getPassword().equals(userPassword));
        Assert.assertTrue(registeredUser.getUserGuid().equals(userGuid.toString()));
    }

    @Test
    public void testGetInvitation_invitationExists_returnInvitation() {
        SecurityCode sc = mock(SecurityCode.class);
        doReturn(USER_EMAIL).when(sc).getEmail();
        doReturn(sc).when(securityCodeService).verify(Matchers.anyString());

        InvitationModel result = sut.getInvitation("");

        Assert.assertEquals(sc.getEmail(), result.getEmail());
    }

    @Test(expected = EntityNotFoundException.class)
    public void testGetInvitation_invitationExists_throwInvalidSecurityCode() {
        doThrow(new InvalidSecurityCodeException("")).when(securityCodeService).verify(Matchers.anyString());

        sut.getInvitation("");
    }

}
