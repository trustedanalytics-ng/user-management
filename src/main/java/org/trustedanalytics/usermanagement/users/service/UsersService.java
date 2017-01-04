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

import org.trustedanalytics.usermanagement.users.model.User;
import org.trustedanalytics.usermanagement.users.model.UserRequest;
import org.trustedanalytics.usermanagement.users.model.UserRole;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface UsersService {
    Collection<User> getOrgUsers(String orgGuid);

    Optional<User> addOrgUser(UserRequest userRequest, String org, String currentUser);

    void deleteUserFromOrg(UUID userGuid, String orgId);

    UserRole updateOrgUserRole(UUID userGuid, String orgGuid, UserRole role);

    void updateUserRolesInOrgs(String username, UUID uuid);
}
