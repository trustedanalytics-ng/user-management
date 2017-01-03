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

import org.cloudfoundry.identity.uaa.rest.SearchResults;
import org.cloudfoundry.identity.uaa.scim.ScimGroup;
import org.cloudfoundry.identity.uaa.scim.ScimGroupMember;
import org.cloudfoundry.identity.uaa.scim.ScimUser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.trustedanalytics.uaa.UaaOperations;
import org.trustedanalytics.uaa.UserIdNamePair;
import org.trustedanalytics.usermanagement.common.EntityNotFoundException;
import org.trustedanalytics.usermanagement.invitations.service.AccessInvitationsService;
import org.trustedanalytics.usermanagement.invitations.service.InvitationsService;
import org.trustedanalytics.usermanagement.orgs.model.Org;
import org.trustedanalytics.usermanagement.users.model.User;
import org.trustedanalytics.usermanagement.users.model.UserRequest;
import org.trustedanalytics.usermanagement.users.model.UserRole;
import org.trustedanalytics.usermanagement.users.rest.AuthGatewayOperations;
import org.trustedanalytics.usermanagement.users.service.UaaUsersService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UaaUsersServiceTest {

    private User testUser;
    private Collection<User> testUsers;
    private ScimUser testUserFromUaa;
    private ScimGroup adminGroup;
    private List<ScimUser> testUsersFromUaa;
    private Org existingOrganization;

    private UaaUsersService sut;

    @Mock
    private UaaOperations uaaOperations;

    @Mock
    private InvitationsService invitationService;

    @Mock
    private AccessInvitationsService accessInvitationsService;

    @Mock
    private SearchResults<ScimUser> scimUserSearchResults;

    @Mock
    private AuthGatewayOperations authGatewayOperations;

    private final String orgId = "defaultorg";
    private final String userId = "test-user-id";

    class UserComparator implements Comparator<User> {
        @Override
        public int compare(User o1, User o2) {
            return o1.getGuid().compareTo(o2.getGuid());
        }
    }

    @Before
    public void setup() {
        testUser = new User(userId, "testuser", UserRole.USER);
        testUsers = Arrays.asList(testUser);
        testUserFromUaa = new ScimUser(testUser.getGuid(), testUser.getUsername(), "", "");
        adminGroup = new ScimGroup(UUID.randomUUID().toString(), "tap.admin", UUID.randomUUID().toString());
        testUserFromUaa.setGroups(new HashSet<>());
        testUsersFromUaa = Arrays.asList(testUserFromUaa);
        existingOrganization = new Org("defaultorg", "the-only-org");

        sut = new UaaUsersService(uaaOperations, invitationService, accessInvitationsService, authGatewayOperations);
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

        Optional<User> resultUser = sut.addOrgUser(new UserRequest(userToAdd), orgId, currentUser);

        verify(accessInvitationsService).createOrUpdateInvitation(eq(userToAdd), any());
        verify(invitationService).sendInviteEmail(userToAdd, currentUser);
        verify(uaaOperations, never()).createUser(any(), any());
        verify(authGatewayOperations, never()).createUser(any(), any());
        assertFalse(resultUser.isPresent());
    }

    @Test
    public void addOrgUser_userExists_doNotInviteUser_returnUser() {
        UserIdNamePair idNamePair = UserIdNamePair.of(testUser.getGuid(), testUser.getUsername());
        when(uaaOperations.findUserIdByName(testUser.getUsername())).thenReturn(Optional.ofNullable(idNamePair));

        Optional<User> resultUser = sut.addOrgUser(new UserRequest(testUser.getUsername(), UserRole.USER), orgId, "admin_test");

        verify(accessInvitationsService, never()).createOrUpdateInvitation(any(), any());
        verify(authGatewayOperations, times(1)).createUser(orgId, resultUser.get().getGuid());
        assertTrue(resultUser.isPresent());
        assertEquals(testUser, resultUser.get());
    }

    @Test
    public void deleteUserFromOrg_userExists_deleteUser() {
        when(uaaOperations.getUsers()).thenReturn(scimUserSearchResults);
        when(scimUserSearchResults.getResources()).thenReturn(testUsersFromUaa);

        sut.deleteUserFromOrg(testUser.getGuid(), orgId);

        verify(authGatewayOperations, times(1)).deleteUser(orgId, testUser.getGuid());
        verify(uaaOperations).deleteUser(testUser.getGuid());
    }

    @Test(expected = EntityNotFoundException.class)
    public void deleteUserFromOrg_userDoesNotExist_throwEntityNotFound() {
        when(uaaOperations.getUsers()).thenReturn(scimUserSearchResults);
        when(scimUserSearchResults.getResources()).thenReturn(testUsersFromUaa);

        try {
            sut.deleteUserFromOrg("another-user-id", orgId);
        } catch (EntityNotFoundException e) {
            assertEquals(e.getMessage(), "The user does not exist");
            verify(uaaOperations, never()).deleteUser(any());
            throw e;
        }
    }

    @Test
    public void updateOrgUserRole_adminRole_userNotPresentInAdminGroup_addToAdminsGroup() {
        when(uaaOperations.getGroup("tap.admin")).thenReturn(Optional.of(adminGroup));
        adminGroup.setMembers(new ArrayList<>());

        sut.updateOrgUserRole(testUser.getGuid(), orgId, UserRole.ADMIN);

        verify(uaaOperations).addUserToGroup(adminGroup, testUser.getGuid());
        verify(uaaOperations, never()).removeUserFromGroup(any(), any());
    }

    @Test
    public void updateOrgUserRole_adminRole_userPresentInAdminGroup_doNothing() {
        when(uaaOperations.getGroup("tap.admin")).thenReturn(Optional.of(adminGroup));
        adminGroup.setMembers(Arrays.asList(new ScimGroupMember(testUser.getGuid())));

        sut.updateOrgUserRole(testUser.getGuid(), orgId, UserRole.ADMIN);

        verify(uaaOperations, never()).addUserToGroup(any(), any());
        verify(uaaOperations, never()).removeUserFromGroup(any(), any());
    }

    @Test
    public void updateOrgUserRole_userRole_userNotPresentInAdminGroup_doNothing() {
        when(uaaOperations.getGroup("tap.admin")).thenReturn(Optional.of(adminGroup));
        adminGroup.setMembers(new ArrayList<>());

        sut.updateOrgUserRole(testUser.getGuid(), orgId, UserRole.USER);

        verify(uaaOperations, never()).addUserToGroup(any(), any());
        verify(uaaOperations, never()).removeUserFromGroup(any(), any());
    }

    @Test
    public void updateOrgUserRole_userRole_userPresentInAdminsGroup_removeFromAdminsGroup() {
        when(uaaOperations.getGroup("tap.admin")).thenReturn(Optional.of(adminGroup));
        adminGroup.setMembers(Arrays.asList(new ScimGroupMember(testUser.getGuid())));

        sut.updateOrgUserRole(testUser.getGuid(), orgId, UserRole.USER);

        verify(uaaOperations, never()).addUserToGroup(any(), any());
        verify(uaaOperations).removeUserFromGroup(adminGroup, testUser.getGuid());
    }

    @Test(expected = EntityNotFoundException.class)
    public void updateOrgUserRole_groupDoesNotExist_throwEntityNotFound() {
        Optional<ScimGroup> emptyGroup = Optional.empty();
        when(uaaOperations.getGroup(any())).thenReturn(emptyGroup);

        sut.updateOrgUserRole(testUser.getGuid(), orgId, UserRole.ADMIN);
    }
}
