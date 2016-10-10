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

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.trustedanalytics.uaa.ChangePasswordRequest;
import org.trustedanalytics.uaa.UaaOperations;
import org.trustedanalytics.usermanagement.security.service.UserDetailsFinder;
import org.trustedanalytics.usermanagement.users.UserPasswordValidator;
import org.trustedanalytics.usermanagement.users.model.User;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

@RestController
@RequestMapping("/rest/users/current")
public class CurrentUserController {

    private final UaaOperations uaaClient;

    private final UserDetailsFinder detailsFinder;

    private final UserPasswordValidator passwordValidator;

    @Autowired
    public CurrentUserController(UaaOperations uaaClient, UserDetailsFinder detailsFinder, UserPasswordValidator passwordValidator) {
        this.uaaClient = uaaClient;
        this.detailsFinder = detailsFinder;
        this.passwordValidator = passwordValidator;
    }

    @ApiOperation(
            value = "Returns current user.",
            notes = "Privilege level: Any consumer of this endpoint must have a valid access token")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = User.class),
            @ApiResponse(code = 500, message = "Internal server error, e.g. error connecting to CloudController")
    })
    @RequestMapping(method = RequestMethod.GET)
    public User getUser(Authentication auth) {
        return new User(detailsFinder.findUserName(auth), detailsFinder.findUserRole(auth));
    }

    @ApiOperation(
            value = "Changes password for current user.",
            notes = "Privilege level: Any consumer of this endpoint must have a valid access token")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = User.class),
            @ApiResponse(code = 400, message = "Password cannot be empty"),
            @ApiResponse(code = 409, message = "Password too short"),
            @ApiResponse(code = 500, message = "Internal server error, e.g. error connecting to CloudController")
    })
    @RequestMapping(value = "/password", method = PUT,
            produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    public void changePassword(@RequestBody ChangePasswordRequest request, Authentication auth) {
        passwordValidator.validate(request.getNewPassword());
        uaaClient.changePassword(detailsFinder.findUserId(auth), request);
    }

}
