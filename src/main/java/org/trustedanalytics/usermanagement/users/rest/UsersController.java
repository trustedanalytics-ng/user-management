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
package org.trustedanalytics.usermanagement.users.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.trustedanalytics.usermanagement.security.service.UserDetailsFinder;
import org.trustedanalytics.usermanagement.users.BlacklistEmailValidator;
import org.trustedanalytics.usermanagement.users.UserRoleRequestValidator;
import org.trustedanalytics.usermanagement.users.model.User;
import org.trustedanalytics.usermanagement.users.model.UserRequest;
import org.trustedanalytics.usermanagement.users.model.UserRole;
import org.trustedanalytics.usermanagement.users.model.UserRolesRequest;
import org.trustedanalytics.usermanagement.users.service.UsersService;

import java.util.Collection;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
public class UsersController {

    public static final String ORG_USERS_URL = "/rest/orgs/{org}/users";

    private final UsersService usersService;
    private final UsersService priviledgedUsersService;
    private final UserDetailsFinder detailsFinder;
    private final BlacklistEmailValidator emailValidator;

    @Autowired
    public UsersController(UsersService usersService, UsersService priviledgedUsersService,
        UserDetailsFinder detailsFinder, BlacklistEmailValidator emailValidator) {
        this.usersService = usersService;
        this.priviledgedUsersService = priviledgedUsersService;
        this.detailsFinder = detailsFinder;
        this.emailValidator = emailValidator;
    }

    private UsersService determinePriviledgeLevel(Authentication auth) {
        if (detailsFinder.findUserRole(auth).equals(UserRole.ADMIN)) {
            return priviledgedUsersService;
        }
        return usersService;
    }

    @ApiOperation(
            value = "Returns list of users which has at least one role in the organization. NOTE: The CF role " +
                    "'Users' is not included ",
            notes = "Privilege level: Consumer of this endpoint must be a member of specified organization based on " +
                    "valid access token")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK", response = User.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Request was malformed. eg. organization with ID 'org' doesn't exist"),
        @ApiResponse(code = 500, message = "Internal server error, e.g. error connecting to CloudController")
    })
    @RequestMapping(value = ORG_USERS_URL, method = GET, produces = APPLICATION_JSON_VALUE)
    public Collection<User> getOrgUsers(@PathVariable String org, @ApiParam(hidden = true) Authentication auth) {
        return determinePriviledgeLevel(auth).getOrgUsers(org);
    }

    @ApiOperation(
            value = "Sends invitations message for new users or returns user for existing one in organization.",
            notes = "Privilege level: Consumer of this endpoint must be a member of specified organization " +
                    "with OrgManager role, based on valid access token"
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = User.class),
            @ApiResponse(code = 400, message = "Request was malformed. e.g. organization with ID 'org' doesn't exist"),
            @ApiResponse(code = 409, message = "Email is not valid or it belongs to forbidden domains."),
            @ApiResponse(code = 500, message = "Internal server error, e.g. error connecting to CloudController")
    })
    @RequestMapping(value = ORG_USERS_URL, method = POST,
            produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    public User createOrgUser(@RequestBody UserRequest userRequest, @PathVariable String org,
                              @ApiParam(hidden = true) Authentication auth) {
        String userPerformingRequestGuid = detailsFinder.findUserName(auth);
        emailValidator.validate(userRequest.getUsername());
        return determinePriviledgeLevel(auth).addOrgUser(userRequest, org, userPerformingRequestGuid ).orElse(null);
    }

    @ApiOperation(
            value = "Updates user roles in organization",
            notes = "Privilege level: Consumer of this endpoint must be a member of specified organization " +
                    "with OrgManager role, based on valid access token")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = UserRole.class, responseContainer = "List"),
            @ApiResponse(code = 400, message = "Request was malformed. e.g. organization with ID 'org' doesn't exist"),
            @ApiResponse(code = 404, message = "User not found in organization."),
            @ApiResponse(code = 409, message = "Roles should be specified."),
            @ApiResponse(code = 500, message = "Internal server error, e.g. error connecting to CloudController")
    })
    @RequestMapping(value = ORG_USERS_URL+"/{user}", method = POST,
            produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    public UserRole updateOrgUserRole(@RequestBody UserRolesRequest userRolesRequest, @PathVariable String org,
                                         @PathVariable String user, @ApiParam(hidden = true) Authentication auth) {
        UserRoleRequestValidator.validate(userRolesRequest);
        String userPerformingRequestGuid = detailsFinder.findUserId(auth);
        denyOperationsOnYourself(userPerformingRequestGuid, user);
        return determinePriviledgeLevel(auth)
                .updateOrgUserRole(user, org, userRolesRequest.getRole());
    }

    @ApiOperation(
            value = "Deletes user from organization.",
            notes = "Privilege level: Consumer of this endpoint must be a member of specified organization " +
                    "with OrgManager role, based on valid access token")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 400, message = "Request was malformed. e.g. organization with ID 'org' doesn't exist"),
            @ApiResponse(code = 404, message = "User 'user' not found in organization."),
            @ApiResponse(code = 500, message = "Internal server error, e.g. error connecting to CloudController")
    })
    @RequestMapping(value = ORG_USERS_URL+"/{user}", method = DELETE)
    public void deleteUserFromOrg(@PathVariable String org, @PathVariable String user,
                                  @ApiParam(hidden = true) Authentication auth) {
        String userPerformingRequestGuid = detailsFinder.findUserId(auth);
        denyOperationsOnYourself(userPerformingRequestGuid, user);
        determinePriviledgeLevel(auth).deleteUserFromOrg(user, org);
    }

    private void denyOperationsOnYourself(String operationPerformer, String userEntity){
        if(operationPerformer.equals(userEntity)){
            throw new AccessDeniedException("You cannot perform request on yourself.");
        }
    }
}
