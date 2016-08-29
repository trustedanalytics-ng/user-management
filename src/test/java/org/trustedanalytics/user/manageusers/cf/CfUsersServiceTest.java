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

import org.cloudfoundry.identity.uaa.rest.SearchResults;
import org.cloudfoundry.identity.uaa.scim.ScimUser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.trustedanalytics.uaa.UaaOperations;
import org.trustedanalytics.uaa.UserIdNamePair;
import org.trustedanalytics.user.common.EntityNotFoundException;
import org.trustedanalytics.user.invite.InvitationsService;
import org.trustedanalytics.user.invite.access.AccessInvitationsService;
import org.trustedanalytics.user.manageusers.CfUsersService;
import org.trustedanalytics.user.manageusers.UserRequest;
import org.trustedanalytics.user.model.Org;
import org.trustedanalytics.user.model.User;
import org.trustedanalytics.user.model.UserRole;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CfUsersServiceTest {

    private User testUser;
    private Collection<User> testUsers;
    private ScimUser testUserFromUaa;
    private Set<ScimUser.Group> testUserGroups;
    private List<ScimUser> testUsersFromUaa;
    private Org existingOrganization;

    private CfUsersService sut;

    @Mock
    private UaaOperations uaaOperations;

    @Mock
    private InvitationsService invitationService;

    @Mock
    private AccessInvitationsService accessInvitationsService;

    @Mock
    private SearchResults<ScimUser> scimUserSearchResults;

    class UserComparator implements Comparator<User> {
        @Override
        public int compare(User o1, User o2) {
            return o1.getGuid().compareTo(o2.getGuid());
        }
    }

    @Before
    public void setup() {
        testUser = new User(UUID.randomUUID(), "testuser", UserRole.USER);
        testUsers = Arrays.asList(testUser);
        testUserFromUaa = new ScimUser(testUser.getGuid().toString(), testUser.getUsername(), "", "");
        testUserGroups = new HashSet<>(Arrays.asList(new ScimUser.Group("tap,admin", "fake")));
        testUserFromUaa.setGroups(new HashSet<>());
        testUsersFromUaa = Arrays.asList(testUserFromUaa);
        existingOrganization = new Org(UUID.randomUUID(), "the-only-org");

        sut = new CfUsersService(uaaOperations, invitationService, accessInvitationsService);
    }

    @Test
    public void getOrgUsers_uaaReturnsUsers_resultContainsAllUsersFromUaa() {
        when(uaaOperations.getUsers()).thenReturn(scimUserSearchResults);
        when(scimUserSearchResults.getResources()).thenReturn(testUsersFromUaa);

        Collection<User> result = sut.getOrgUsers(existingOrganization.getGuid());

        assertTrue(result.containsAll(testUsers));
    }

    @Test
    public void addOrgUser_userDoesntExist_inviteUser_doNotCreateAccount_returnEmptyOptional() {
        String userToAdd = "testuser";
        String currentUser = "admin_test";
        when(uaaOperations.findUserIdByName(userToAdd)).thenReturn(Optional.empty());
        when(accessInvitationsService.createOrUpdateInvitation(eq(userToAdd),
                any())).thenReturn(AccessInvitationsService.CreateOrUpdateState.CREATED);

        Optional<User> resultUser = sut.addOrgUser(new UserRequest(userToAdd), UUID.randomUUID(), currentUser);

        verify(accessInvitationsService).createOrUpdateInvitation(eq(userToAdd), any());
        verify(invitationService).sendInviteEmail(userToAdd, currentUser);
        verify(uaaOperations, never()).createUser(any(), any());
        assertFalse(resultUser.isPresent());
    }

    @Test
    public void addOrgUser_userExists_doNotInviteUser_returnUser() {
        UserIdNamePair idNamePair = UserIdNamePair.of(testUser.getGuid(), testUser.getUsername());
        when(uaaOperations.findUserIdByName(testUser.getUsername())).thenReturn(Optional.ofNullable(idNamePair));

        Optional<User> resultUser =
                sut.addOrgUser(new UserRequest(testUser.getUsername(), UserRole.USER), UUID.randomUUID(), "admin_test");

        verify(accessInvitationsService, never()).createOrUpdateInvitation(any(), any());
        assertTrue(resultUser.isPresent());
        assertEquals(testUser, resultUser.get());
    }

    @Test
    public void deleteUserFromOrg_userExists_deleteUser() {
        when(uaaOperations.getUsers()).thenReturn(scimUserSearchResults);
        when(scimUserSearchResults.getResources()).thenReturn(testUsersFromUaa);

        sut.deleteUserFromOrg(testUser.getGuid(), UUID.randomUUID());

        verify(uaaOperations).deleteUser(testUser.getGuid());
    }

    @Test(expected = EntityNotFoundException.class)
    public void deleteUserFromOrg_userDoesNotExist_throwEntityNotFound() {
        when(uaaOperations.getUsers()).thenReturn(scimUserSearchResults);
        when(scimUserSearchResults.getResources()).thenReturn(testUsersFromUaa);

        try {
            sut.deleteUserFromOrg(UUID.randomUUID(), UUID.randomUUID());
        } catch (EntityNotFoundException e) {
            assertEquals(e.getMessage(), "The user does not exist");
            verify(uaaOperations, never()).deleteUser(any());
            throw e;
        }
    }

    @Test(expected = NotImplementedException.class)
    public void updateOrgUserRoles_throwNotImplemented() {
        sut.updateOrgUserRole(UUID.randomUUID(), UUID.randomUUID(), UserRole.ADMIN);
    }
}
