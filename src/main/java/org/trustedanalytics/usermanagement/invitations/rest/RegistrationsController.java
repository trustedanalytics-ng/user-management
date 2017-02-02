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
package org.trustedanalytics.usermanagement.invitations.rest;

import com.google.common.base.Strings;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.trustedanalytics.usermanagement.common.EntityNotFoundException;
import org.trustedanalytics.usermanagement.invitations.model.Invitation;
import org.trustedanalytics.usermanagement.invitations.model.Registration;
import org.trustedanalytics.usermanagement.invitations.securitycode.InvalidSecurityCodeException;
import org.trustedanalytics.usermanagement.invitations.securitycode.SecurityCode;
import org.trustedanalytics.usermanagement.invitations.securitycode.SecurityCodeService;
import org.trustedanalytics.usermanagement.invitations.service.AccessInvitationsService;
import org.trustedanalytics.usermanagement.invitations.service.InvitationsService;
import org.trustedanalytics.usermanagement.orgs.model.Org;
import org.trustedanalytics.usermanagement.orgs.service.OrganizationsStorage;
import org.trustedanalytics.usermanagement.users.UserPasswordValidator;
import org.trustedanalytics.usermanagement.users.service.UsersService;

@RestController
@RequestMapping("/rest/registrations")
public class RegistrationsController {

    private final SecurityCodeService securityCodeService;
    private final InvitationsService invitationsService;
    private final AccessInvitationsService accessInvitationsService;
    private final UserPasswordValidator userPasswordValidator;
    private final UsersService privilegedUsersService;
    private final OrganizationsStorage organizationsStorage;

    @Autowired
    public RegistrationsController(SecurityCodeService securityCodeService,
                                   InvitationsService invitationsService,
                                   AccessInvitationsService accessInvitationsService,
                                   UserPasswordValidator userPasswordValidator,
                                   UsersService privilegedUsersService,
                                   OrganizationsStorage organizationsStorage) {
        this.securityCodeService = securityCodeService;
        this.invitationsService = invitationsService;
        this.accessInvitationsService = accessInvitationsService;
        this.userPasswordValidator = userPasswordValidator;
        this.privilegedUsersService = privilegedUsersService;
        this.organizationsStorage = organizationsStorage;
    }

    @ApiOperation(
            value = "Registers new user using security code received in email message.",
            notes = "Privilege level: Consumer of this endpoint requires a valid one-time security code")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = Registration.class),
            @ApiResponse(code = 400, message = "Invalid organization name."),
            @ApiResponse(code = 403, message = "Security code 'code' empty or null"),
            @ApiResponse(code = 409, message = "Invalid password (empty or too short)."),
            @ApiResponse(code = 409, message = "Org already exists."),
            @ApiResponse(code = 409, message = "User already exists."),
            @ApiResponse(code = 500, message = "Internal server error, e.g. error connecting to CloudController")
    })
    @RequestMapping(method = RequestMethod.POST)
    public Registration addUser(@RequestBody Registration newUser,
                                @RequestParam(value = "code", required = false) String code) {

        // TODO: missing multi-organization feature. User should have eligibility to create organization.
        Org organizationInvitedTo = organizationsStorage.getOrganizations().iterator().next();

        if (Strings.isNullOrEmpty(code)) {
            throw new InvalidSecurityCodeException("Security code empty or null");
        }
        SecurityCode sc = securityCodeService.verify(code);
        userPasswordValidator.validate(newUser.getPassword());
        String email = sc.getEmail();
        invitationsService
            .createUser(email, newUser.getPassword(), organizationInvitedTo.getGuid())
            .ifPresent(uuid -> {
                newUser.setUserGuid(uuid);
                privilegedUsersService.updateUserRolesInOrgs(email, uuid);
            });

        securityCodeService.redeem(sc);
        accessInvitationsService.redeemAccessInvitations(email);

        return newUser;
    }

    @ApiOperation(
            value = "Gets invitation using security code received in email message.",
            notes = "Privilege level: Consumer of this endpoint requires a valid one-time security code")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = Invitation.class),
            @ApiResponse(code = 403, message = "Security code 'code' empty or null"),
            @ApiResponse(code = 500, message = "Internal server error, e.g. error connecting to CloudController")
    })
    @RequestMapping(value = "/{code}", method = RequestMethod.GET)
    public Invitation getInvitation(@PathVariable("code") String code) {
        try {
            final SecurityCode sc = securityCodeService.verify(code);
            return Invitation.of(sc.getEmail());
        } catch (InvalidSecurityCodeException e) {
            throw new EntityNotFoundException("", e);
        }
    }
}
