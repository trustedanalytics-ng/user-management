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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.security.core.Authentication;
import org.trustedanalytics.usermanagement.invitations.model.Invitation;
import org.trustedanalytics.usermanagement.invitations.model.InvitationErrorDescription;
import org.trustedanalytics.usermanagement.invitations.rest.InvitationsController;
import org.trustedanalytics.usermanagement.invitations.securitycode.SecurityCodeService;
import org.trustedanalytics.usermanagement.invitations.service.AccessInvitations;
import org.trustedanalytics.usermanagement.invitations.service.AccessInvitationsService;
import org.trustedanalytics.usermanagement.invitations.service.InvitationsService;
import org.trustedanalytics.usermanagement.orgs.model.Org;
import org.trustedanalytics.usermanagement.orgs.service.OrganizationsStorage;
import org.trustedanalytics.usermanagement.orgs.service.SingleOrganizationStorage;
import org.trustedanalytics.usermanagement.security.service.UserDetailsFinder;
import org.trustedanalytics.usermanagement.users.BlacklistEmailValidator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class InvitationsControllerTest {

    private static final String ADMIN_EMAIL = "admin@example.com";
    private static final String USER_EMAIL = "email@example.com";
    private static final String USER_EMAIL_UPPER_CASE = "EMAIL@EXAMPLEE.COM";
    private List<String> forbiddenDomains = new ArrayList<>();
    private BlacklistEmailValidator emailValidator = new BlacklistEmailValidator(forbiddenDomains);

    private InvitationsController sut;

    @Mock
    private InvitationsService invitationsService;

    @Mock
    private SecurityCodeService securityCodeService;

    @Mock
    private UserDetailsFinder detailsFinder;

    @Mock
    private AccessInvitationsService accessInvitationsService;

    private OrganizationsStorage organizationsStorage;

    @Before
    public void setUp() throws Exception {
        organizationsStorage = new SingleOrganizationStorage("sample-org-id", "sample-org-name");
        sut = new InvitationsController(invitationsService, detailsFinder, accessInvitationsService, emailValidator, organizationsStorage);
    }

    @Test
    public void testAddInvitation_userDoesNotExistNoPendingInvitation_sendEmail_AddInvitation() {
        Invitation invitation = Invitation.of(USER_EMAIL);
        doReturn(ADMIN_EMAIL).when(detailsFinder).findUserName(any(Authentication.class));

        when(invitationsService.userExists(anyString())).thenReturn(false);
        when(accessInvitationsService.getAccessInvitations(anyString())).thenReturn(Optional.empty());

        sut.addInvitation(invitation, null);

        verify(invitationsService).sendInviteEmail(eq(USER_EMAIL), eq(ADMIN_EMAIL));
        verify(accessInvitationsService).createOrUpdateInvitation(eq(USER_EMAIL), any());
    }

    @Test
    public void testAddInvitation_userEmailUpperCase_accepted() {
        Invitation invitation = Invitation.of(USER_EMAIL_UPPER_CASE);

        when(invitationsService.userExists(anyString())).thenReturn(false);
        when(accessInvitationsService.getAccessInvitations(anyString())).thenReturn(Optional.empty());

        sut.addInvitation(invitation, null);
    }

    @Test(expected = UserExistsException.class)
    public void testAddInvitation_userExist_throwUserExistsException() {
        Invitation invitation = Invitation.of(USER_EMAIL);
        doReturn(ADMIN_EMAIL).when(detailsFinder).findUserName(any(Authentication.class));

        when(invitationsService.userExists(anyString())).thenReturn(true);
        when(accessInvitationsService.getAccessInvitations(anyString())).thenReturn(Optional.empty());

        sut.addInvitation(invitation, null);
    }

    @Test
    public void testAddInvitation_userDoesNotExisInvitationPending_addEglibilityToCreateOrg() {
        Invitation invitation = Invitation.of(USER_EMAIL);
        doReturn(ADMIN_EMAIL).when(detailsFinder).findUserName(any(Authentication.class));

        when(invitationsService.userExists(anyString())).thenReturn(false);
        AccessInvitations accessInvitation = new AccessInvitations();
        when(accessInvitationsService.getAccessInvitations(anyString())).thenReturn(Optional.of(accessInvitation));

        InvitationErrorDescription result =  sut.addInvitation(invitation, null);

        ArgumentCaptor<AccessInvitations> captor = new ArgumentCaptor<>();

        verify(accessInvitationsService).updateAccessInvitation(anyString(), captor.capture());

        assertEquals("Updated pending invitation", result.getDetails());
    }

    @Test(expected = WrongEmailAddressException.class)
    public void testAddInvitation_WrongEmailAddress() {
        String invalidEmail = "invalidEmail";
        Invitation invitation = Invitation.of(invalidEmail);
        sut.addInvitation(invitation, null);
    }
}
