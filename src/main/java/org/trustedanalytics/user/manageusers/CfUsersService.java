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
package org.trustedanalytics.user.manageusers;

import org.trustedanalytics.uaa.UaaOperations;
import org.trustedanalytics.uaa.UserIdNamePair;
import org.trustedanalytics.user.common.EntityNotFoundException;
import org.trustedanalytics.user.current.AuthDetailsFinder;
import org.trustedanalytics.user.invite.InvitationsService;
import org.trustedanalytics.user.invite.access.AccessInvitations;
import org.trustedanalytics.user.invite.access.AccessInvitationsService;
import org.trustedanalytics.user.model.User;
import org.trustedanalytics.user.model.UserRole;

import org.cloudfoundry.identity.uaa.scim.ScimGroup;
import org.cloudfoundry.identity.uaa.scim.ScimUser;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class CfUsersService implements UsersService {

    private final UaaOperations uaaClient;
    private final InvitationsService invitationsService;
    private final AccessInvitationsService accessInvitationsService;
    private final AuthGatewayOperations authGatewayOperations;

    public CfUsersService(UaaOperations uaaClient,
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
    public Collection<User> getOrgUsers(UUID orgGuid) {
        Collection<ScimUser> scimUsers = uaaClient.getUsers().getResources();
        return scimUsers.stream()
                .map(scimUser -> new User(UUID.fromString(scimUser.getId()), scimUser.getUserName(),
                        extractOrgRole(scimUser)))
                .collect(Collectors.toList());
    }

    private UserRole extractOrgRole(ScimUser user) {
        if (user.getGroups().stream().anyMatch(g -> g.getDisplay().equals(AuthDetailsFinder.ADMIN_GROUP))) {
            return UserRole.ADMIN;
        }
        return UserRole.USER;
    }

    @Override
    public Optional<User> addOrgUser(UserRequest userRequest, UUID orgGuid, String currentUser) {
        Optional<UserIdNamePair> idNamePair = uaaClient.findUserIdByName(userRequest.getUsername());
        if(!idNamePair.isPresent()) {
            UserRole role = Optional.ofNullable(userRequest.getRole()).orElse(UserRole.USER);
            inviteUserToOrg(userRequest.getUsername(), currentUser, orgGuid, role);
        }
        return idNamePair.map(pair -> {
            UUID userGuid = pair.getGuid();
            authGatewayOperations.createUser(orgGuid.toString(), userGuid.toString());
            return new User(userGuid, userRequest.getUsername(), userRequest.getRole());
        });
    }

    private void inviteUserToOrg(String username, String currentUser, UUID orgGuid, UserRole role) {
        AccessInvitationsService.CreateOrUpdateState state =
                accessInvitationsService.createOrUpdateInvitation(username, ui -> ui.addOrgAccessInvitation(orgGuid, role));
        if (state == AccessInvitationsService.CreateOrUpdateState.CREATED) {
            invitationsService.sendInviteEmail(username, currentUser);
        }
    }

    @Override
    public void deleteUserFromOrg(UUID userGuid, UUID orgGuid) {
        if (getOrgUsers(orgGuid).stream().noneMatch(x -> userGuid.equals(x.getGuid()))) {
            throw new EntityNotFoundException("The user does not exist", null);
        }
        uaaClient.deleteUser(userGuid);
        authGatewayOperations.deleteUser(orgGuid.toString(), userGuid.toString());
    }

    @Override
    public UserRole updateOrgUserRole(UUID userGuid, UUID orgGuid, UserRole role) {
        ScimGroup adminGroup = getAdminGroup();
        if (isGroupMember(adminGroup, userGuid) && role.equals(UserRole.USER)) {
            uaaClient.removeUserFromGroup(adminGroup, userGuid);
        } else if (!isGroupMember(adminGroup, userGuid) && role.equals(UserRole.ADMIN)) {
            uaaClient.addUserToGroup(adminGroup, userGuid);
        }
        return role;
    }

    @Override
    public void updateUserRolesInOrgs(String username, UUID uuid){
        accessInvitationsService
            .getAccessInvitations(username)
            .map(AccessInvitations::getOrgAccessInvitations)
            .orElse(Collections.emptyMap())
            .forEach((orgGuid, role) ->
                    updateOrgUserRole(uuid, orgGuid, role));
    }

    private ScimGroup getAdminGroup() {
        return uaaClient
                .getGroup(AuthDetailsFinder.ADMIN_GROUP)
                .orElseThrow(() -> new EntityNotFoundException("Group " + AuthDetailsFinder.ADMIN_GROUP +
                " not found in UAA database"));
    }

    private boolean isGroupMember(ScimGroup group, UUID userGuid) {
        return group.getMembers().stream().anyMatch(m -> m.getMemberId().equals(userGuid.toString()));
    }
}
