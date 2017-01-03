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
package org.trustedanalytics.usermanagement.invitations;

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
import org.trustedanalytics.usermanagement.common.EntityNotFoundException;
import org.trustedanalytics.usermanagement.invitations.model.Invitation;
import org.trustedanalytics.usermanagement.invitations.model.Registration;
import org.trustedanalytics.usermanagement.invitations.rest.RegistrationsController;
import org.trustedanalytics.usermanagement.invitations.securitycode.InvalidSecurityCodeException;
import org.trustedanalytics.usermanagement.invitations.securitycode.SecurityCode;
import org.trustedanalytics.usermanagement.invitations.securitycode.SecurityCodeService;
import org.trustedanalytics.usermanagement.invitations.service.AccessInvitations;
import org.trustedanalytics.usermanagement.invitations.service.AccessInvitationsService;
import org.trustedanalytics.usermanagement.invitations.service.InvitationsService;
import org.trustedanalytics.usermanagement.users.EmptyPasswordException;
import org.trustedanalytics.usermanagement.users.TooShortPasswordException;
import org.trustedanalytics.usermanagement.users.UserPasswordValidator;
import org.trustedanalytics.usermanagement.users.model.UserRole;
import org.trustedanalytics.usermanagement.users.service.UsersService;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RegistrationsControllerTest {

    private static final String USER_EMAIL = "email@example.com";
    private static final String SECURITY_CODE = "code";
    private static final String CORE_GUID = "defaultorg";

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
        sut.addUser(new Registration(), null);
    }

    @Test(expected = InvalidSecurityCodeException.class)
    public void testAddUser_emptyCodeGiven_throwInvalidCode() {
        sut.addUser(new Registration(), "");
    }

    @Test(expected = InvalidSecurityCodeException.class)
    public void testAddUser_securityCodeInvalid_throwInvalidCode() {
        doThrow(new InvalidSecurityCodeException("")).when(securityCodeService).verify(Matchers.anyString());

        sut.addUser(new Registration(), SECURITY_CODE);
    }

    @Test(expected = TooShortPasswordException.class)
    public void testAddUser_passwordTooShort_throwTooShortPassword() {
        SecurityCode sc = new SecurityCode(USER_EMAIL, SECURITY_CODE);
        doReturn(sc).when(securityCodeService).verify(Matchers.anyString());
        Registration registration = new Registration();
        registration.setPassword("123");

        sut.addUser(registration, SECURITY_CODE);
    }

    @Test(expected = EmptyPasswordException.class)
    public void testAddUser_passwordEmpty_throwEmptyPassword() {
        SecurityCode sc = new SecurityCode(USER_EMAIL, SECURITY_CODE);
        doReturn(sc).when(securityCodeService).verify(Matchers.anyString());
        Registration registration = new Registration();
        registration.setPassword("");

        sut.addUser(registration, SECURITY_CODE);
    }

    @Test(expected = UserExistsException.class)
    public void testAddUser_createUserAlreadyExists_throwUserExistsException() {
        SecurityCode sc = new SecurityCode(USER_EMAIL, SECURITY_CODE);
        doReturn(sc).when(securityCodeService).verify(Matchers.anyString());
        Registration registration = new Registration();
        registration.setPassword("123456");


        doThrow(new UserExistsException("")).when(invitationsService).createUser(
                Matchers.anyString(), Matchers.anyString());

        sut.addUser(registration, SECURITY_CODE);
    }

    @Test(expected = OrgExistsException.class)
    public void testAddUser_createUserAlreadyExistsNoOrg_throwOrgExistsException() {
        SecurityCode sc = new SecurityCode(USER_EMAIL, SECURITY_CODE);
        doReturn(sc).when(securityCodeService).verify(Matchers.anyString());
        Registration registration = new Registration();
        registration.setPassword("123456");
        doThrow(new OrgExistsException("")).when(invitationsService).createUser(
                Matchers.anyString(), Matchers.anyString());

        sut.addUser(registration, SECURITY_CODE);
    }

    @Test(expected = HttpClientErrorException.class)
    public void testAddUser_createUserHttpConnectionError_throwHttpError() {
        SecurityCode sc = new SecurityCode(USER_EMAIL, SECURITY_CODE);
        doReturn(sc).when(securityCodeService).verify(Matchers.anyString());
        Registration registration = new Registration();
        registration.setPassword("123456");
        doThrow(new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR)).when(invitationsService).createUser(
                Matchers.anyString(), Matchers.anyString());

        sut.addUser(registration, SECURITY_CODE);
    }

    @Test(expected = InvalidOrganizationNameException.class)
    public void testAddUser_createUserNoOrgHttpConnectionError_throwHttpError() {
        SecurityCode sc = new SecurityCode(USER_EMAIL, SECURITY_CODE);
        doReturn(sc).when(securityCodeService).verify(Matchers.anyString());
        Registration registration = new Registration();
        registration.setPassword("123456");
        doThrow(new InvalidOrganizationNameException("")).when(invitationsService).createUser(
                Matchers.anyString(), Matchers.anyString());

        sut.addUser(registration, SECURITY_CODE);
    }

    @Test()
    public void testAddUser_allOk_useCode() {
        SecurityCode sc = new SecurityCode(USER_EMAIL, SECURITY_CODE);
        doReturn(sc).when(securityCodeService).verify(Matchers.anyString());
        Registration registration = new Registration();
        String userPassword = "123456";
        String userGuid = "test-user-id";
        when(invitationsService.createUser(USER_EMAIL, userPassword)).thenReturn(Optional.of(userGuid));

        AccessInvitations accessInvitations = new AccessInvitations();
        accessInvitations.getOrgAccessInvitations().put(CORE_GUID, UserRole.ADMIN);
        when(accessInvitationsService
                .getAccessInvitations(USER_EMAIL)).thenReturn(Optional.of(accessInvitations));
        registration.setPassword(userPassword);

        Registration registeredUser = sut.addUser(registration, SECURITY_CODE);

        Mockito.verify(securityCodeService).redeem(sc);
        Assert.assertTrue(registeredUser.getPassword().equals(userPassword));
        assertThat(registeredUser.getUserGuid(), equalTo(userGuid));
    }

    @Test
    public void testGetInvitation_invitationExists_returnInvitation() {
        SecurityCode sc = mock(SecurityCode.class);
        doReturn(USER_EMAIL).when(sc).getEmail();
        doReturn(sc).when(securityCodeService).verify(Matchers.anyString());

        Invitation result = sut.getInvitation("");

        Assert.assertEquals(sc.getEmail(), result.getEmail());
    }

    @Test(expected = EntityNotFoundException.class)
    public void testGetInvitation_invitationExists_throwInvalidSecurityCode() {
        doThrow(new InvalidSecurityCodeException("")).when(securityCodeService).verify(Matchers.anyString());

        sut.getInvitation("");
    }

}
