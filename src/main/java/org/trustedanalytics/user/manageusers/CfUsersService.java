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

import org.cloudfoundry.identity.uaa.scim.ScimUser;
import org.trustedanalytics.uaa.UaaOperations;
import org.trustedanalytics.uaa.UserIdNamePair;
import org.trustedanalytics.user.common.EntityNotFoundException;
import org.trustedanalytics.user.invite.InvitationsService;
import org.trustedanalytics.user.invite.access.AccessInvitationsService;
import org.trustedanalytics.user.model.OrgRole;
import org.trustedanalytics.user.model.UserModel;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class CfUsersService implements UsersService {

    private final UaaOperations uaaClient;
    private final InvitationsService invitationsService;
    private final AccessInvitationsService accessInvitationsService;

    public CfUsersService(UaaOperations uaaClient,
                          InvitationsService invitationsService,
                          AccessInvitationsService accessInvitationsService) {
        super();
        this.uaaClient = uaaClient;
        this.invitationsService = invitationsService;
        this.accessInvitationsService = accessInvitationsService;
    }

    @Override
    public Collection<UserModel> getOrgUsers(UUID orgGuid) {
        Collection<ScimUser> scimUsers = uaaClient.getUsers().getResources();
        return scimUsers.stream()
                .map(scimUser -> new UserModel(UUID.fromString(scimUser.getId()), scimUser.getUserName()))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<UserModel> addOrgUser(UserRequest userRequest, UUID orgGuid, String currentUser) {
        // TODO: org roles are currently not supported in UAA
        Optional<UserIdNamePair> idNamePair = uaaClient.findUserIdByName(userRequest.getUsername());
        if(!idNamePair.isPresent()) {
            inviteUserToOrg(userRequest.getUsername(),
                    currentUser, orgGuid, new HashSet<>(Arrays.asList(OrgRole.USERS)));
        }
        return idNamePair.map(pair -> {
            UUID userGuid = pair.getGuid();
            return new UserModel(userGuid, userRequest.getUsername());
        });
    }

    private void inviteUserToOrg(String username, String currentUser, UUID orgGuid, Set<OrgRole> roles) {

        AccessInvitationsService.CreateOrUpdateState state =
                accessInvitationsService.createOrUpdateInvitation(username, ui -> ui.addOrgAccessInvitation(orgGuid, roles));
        if (state == AccessInvitationsService.CreateOrUpdateState.CREATED) {
            invitationsService.sendInviteEmail(username, currentUser);
        }
    }

    @Override
    public List<OrgRole> updateOrgUserRoles(UUID userGuid, UUID orgGuid, UserRolesRequest userRolesRequest) {
        // TODO: org roles are currently not supported in UAA
        throw new NotImplementedException();
    }

    @Override
    public void deleteUserFromOrg(UUID userGuid, UUID orgGuid) {
        if (getOrgUsers(orgGuid).stream().noneMatch(x -> userGuid.equals(x.getGuid()))) {
            throw new EntityNotFoundException("The user does not exist", null);
        }
        uaaClient.deleteUser(userGuid);
    }

    @Override
    public boolean isOrgAdmin(UUID userId, UUID orgId) {
        // TODO: org roles are currently not supported in UAA
        return false;
    }
}
