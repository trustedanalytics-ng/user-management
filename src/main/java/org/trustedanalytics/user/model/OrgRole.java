/**
 *  Copyright (c) 2016 Intel Corporation 
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
package org.trustedanalytics.user.model;

import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;


public enum OrgRole {
    ADMINS("admins"),
    USERS("users"); //every user has to be in that role along any other

    public static final Set<OrgRole> ORG_ROLES = EnumSet.of(ADMINS, USERS);
    private final String roleName;
    private static final Map<String, OrgRole> ROLES_MAP = ImmutableMap.<String, OrgRole>builder()
            .put("org_user", OrgRole.USERS)
            .put("org_admin", OrgRole.ADMINS)
            .build();

    private OrgRole(String roleName) {
        this.roleName = roleName;
    }

    @JsonValue
    public String getValue() {
        return roleName;
    }

    public static OrgRole getRoleByName(String roleName) {
        OrgRole role = ROLES_MAP.get(roleName);
        Preconditions.checkNotNull(role, String.format("Role %s is not a known role type", roleName));
        return role;
    }
}
