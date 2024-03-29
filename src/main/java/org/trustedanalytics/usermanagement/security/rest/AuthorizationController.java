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
package org.trustedanalytics.usermanagement.security.rest;

import com.google.common.base.Strings;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.trustedanalytics.usermanagement.orgs.model.Org;
import org.trustedanalytics.usermanagement.orgs.service.OrganizationsStorage;
import org.trustedanalytics.usermanagement.security.model.OrgPermission;
import org.trustedanalytics.usermanagement.security.service.UserDetailsFinder;
import org.trustedanalytics.usermanagement.users.model.UserRole;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
public class AuthorizationController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationController.class);

    private final UserDetailsFinder detailsFinder;
    private final OrganizationsStorage organizationsStorage;

    @Autowired
    public AuthorizationController(UserDetailsFinder detailsFinder, OrganizationsStorage organizationsStorage) {
        this.detailsFinder = detailsFinder;
        this.organizationsStorage = organizationsStorage;
    }

    @ApiOperation(
            value = "Returns permissions for user within one organization",
            notes = "Privilege level: Any consumer of this endpoint must have a valid access token"
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = OrgPermission.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @RequestMapping(value = "/rest/orgs/permissions", method = GET)
    public Collection<OrgPermission> getPermissions(@RequestParam(required = false) String orgs,
        Authentication authentication) {

        final List<String> organizations = new ArrayList<>();
        if (!Strings.isNullOrEmpty(orgs)) {
            organizations.addAll(
                Arrays.stream(orgs.split(",")).collect(toList()));
        }

        return resolvePermissions(organizations, authentication);
    }

    /**
     * Returns permissions for user within one organization
     *
     * @param orgs           collections of organizations
     * @param authentication authentication
     * @return permissions
     */
    private Collection<OrgPermission> resolvePermissions(Collection<String> orgs,
        Authentication authentication) {
        final String user = detailsFinder.findUserId(authentication);
        final UserRole role = detailsFinder.findUserRole(authentication);

        LOGGER.info("Resolving permissions for user: {}", user);
        return UserRole.ADMIN.equals(role) ?
            resolveAdminPermissions(orgs) :
            resolveUserPermissions(user, orgs);
    }

    /**
     * Returns permissions for specified organizations for administrator user. By default
     * administrators have access to every organization.
     *
     * @param orgs organizations
     * @return permissions
     */
    private Collection<OrgPermission> resolveAdminPermissions(Collection<String> orgs) {
        Collection<Org> allOrganizations = organizationsStorage.getOrganizations();

        // TODO: in a single organization mode there is no source of user role in a specific organization.
        // We return a global ADMIN permission for "all" organizations (single organization in this case)
        return allOrganizations.stream()
            .map(org -> new OrgPermission(org, true, true))
            .collect(toList());
    }

    /**
     * Return permissions for specified organizations for regular user.
     *
     * @param user user GUID
     * @param orgs organizations
     * @return permissions
     */
    private Collection<OrgPermission> resolveUserPermissions(String user, Collection<String> orgs) {
        Collection<Org> allOrganizations = organizationsStorage.getOrganizations();

        // TODO: in a single organization mode there is no source of user role in a specific organization.
        // We return a global USER permission for "all" organizations (single organization in this case)
        return allOrganizations.stream()
                .map(org -> new OrgPermission(org, true, false))
                .collect(toList());
    }
}
