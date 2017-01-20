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
package org.trustedanalytics.usermanagement.users.service;

import org.cloudfoundry.identity.uaa.scim.ScimGroup;
import org.cloudfoundry.identity.uaa.scim.ScimUser;
import org.trustedanalytics.uaa.UaaOperations;
import org.trustedanalytics.uaa.UserIdNamePair;
import org.trustedanalytics.usermanagement.common.EntityNotFoundException;
import org.trustedanalytics.usermanagement.invitations.UserExistsException;
import org.trustedanalytics.usermanagement.invitations.service.AccessInvitations;
import org.trustedanalytics.usermanagement.invitations.service.AccessInvitationsService;
import org.trustedanalytics.usermanagement.invitations.service.InvitationsService;
import org.trustedanalytics.usermanagement.security.service.UserDetailsFinderImpl;
import org.trustedanalytics.usermanagement.users.model.User;
import org.trustedanalytics.usermanagement.users.model.UserRequest;
import org.trustedanalytics.usermanagement.users.model.UserRole;
import org.trustedanalytics.usermanagement.users.rest.AuthGatewayOperations;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

public class UaaUsersService implements UsersService {

    private final UaaOperations uaaClient;
    private final InvitationsService invitationsService;
    private final AccessInvitationsService accessInvitationsService;
    private final AuthGatewayOperations authGatewayOperations;

    public UaaUsersService(UaaOperations uaaClient,
                           InvitationsService invitationsService,
                           AccessInvitationsService accessInvitationsService,
                           AuthGatewayOperations authGatewayOperations) {
        super();
        this.uaaClient = uaaClient;
        this.invitationsService = invitationsService;
        this.accessInvitationsService = accessInvitationsService;
        this.authGatewayOperations = authGatewayOperations;
    }

    @Override
    public Collection<User> getOrgUsers(String orgGuid) {
        Collection<ScimUser> scimUsers = uaaClient.getUsers().getResources();
        return scimUsers.stream()
                .map(scimUser -> new User(scimUser.getId(), scimUser.getUserName(),
                        extractOrgRole(scimUser)))
                .collect(Collectors.toList());
    }

    private UserRole extractOrgRole(ScimUser user) {
        if (user.getGroups().stream().anyMatch(g -> g.getDisplay().equals(UserDetailsFinderImpl.ADMIN_GROUP))) {
            return UserRole.ADMIN;
        }
        return UserRole.USER;
    }

    @Override
    public void addOrgUser(UserRequest userRequest, String orgGuid, String currentUser) {
        Optional<UserIdNamePair> idNamePair = uaaClient.findUserIdByName(userRequest.getUsername());
        if(idNamePair.isPresent()) {
            throw new UserExistsException("User already exists!");
        }
        UserRole role = Optional.ofNullable(userRequest.getRole()).orElse(UserRole.USER);
        inviteUserToOrg(userRequest.getUsername(), currentUser, orgGuid, role);
    }

    private void inviteUserToOrg(String username, String currentUser, String orgGuid, UserRole role) {
        AccessInvitationsService.CreateOrUpdateState state =
                accessInvitationsService.createOrUpdateInvitation(username, ui -> ui.addOrgAccessInvitation(orgGuid, role));
        if (state == AccessInvitationsService.CreateOrUpdateState.CREATED) {
            invitationsService.sendInviteEmail(username, currentUser);
        }
    }

    @Override
    public void deleteUserFromOrg(String userGuid, String orgGuid) {
        if (getOrgUsers(orgGuid).stream().noneMatch(x -> userGuid.equals(x.getGuid()))) {
            throw new EntityNotFoundException("The user does not exist", null);
        }
        uaaClient.deleteUser(userGuid);
        authGatewayOperations.deleteUser(orgGuid, userGuid);
    }

    @Override
    public UserRole updateOrgUserRole(String userGuid, String orgGuid, UserRole role) {
        ScimGroup adminGroup = getAdminGroup();
        if (isGroupMember(adminGroup, userGuid) && role.equals(UserRole.USER)) {
            uaaClient.removeUserFromGroup(adminGroup, userGuid);
        } else if (!isGroupMember(adminGroup, userGuid) && role.equals(UserRole.ADMIN)) {
            uaaClient.addUserToGroup(adminGroup, userGuid);
        }
        return role;
    }

    @Override
    public void updateUserRolesInOrgs(String username, String uuid){
        accessInvitationsService
            .getAccessInvitations(username)
            .map(AccessInvitations::getOrgAccessInvitations)
            .orElse(Collections.emptyMap())
            .forEach((orgGuid, role) ->
                    updateOrgUserRole(uuid, orgGuid, role));
    }

    private ScimGroup getAdminGroup() {
        return uaaClient
                .getGroup(UserDetailsFinderImpl.ADMIN_GROUP)
                .orElseThrow(() -> new EntityNotFoundException("Group " + UserDetailsFinderImpl.ADMIN_GROUP +
                " not found in UAA database"));
    }

    private boolean isGroupMember(ScimGroup group, String userGuid) {
        return group.getMembers().stream().anyMatch(m -> m.getMemberId().equals(userGuid));
    }
}
