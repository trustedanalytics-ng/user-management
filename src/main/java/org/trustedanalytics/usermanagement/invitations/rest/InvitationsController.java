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


import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.trustedanalytics.usermanagement.invitations.InvitationNotSentException;
import org.trustedanalytics.usermanagement.invitations.UserExistsException;
import org.trustedanalytics.usermanagement.invitations.model.InvitationErrorDescription;
import org.trustedanalytics.usermanagement.invitations.model.Invitation;
import org.trustedanalytics.usermanagement.invitations.service.AccessInvitationsService;
import org.trustedanalytics.usermanagement.invitations.service.InvitationsService;
import org.trustedanalytics.usermanagement.orgs.mocks.OrgResourceMock;
import org.trustedanalytics.usermanagement.security.service.UserDetailsFinder;
import org.trustedanalytics.usermanagement.users.BlacklistEmailValidator;
import org.trustedanalytics.usermanagement.users.model.UserRole;

import java.util.Set;
import java.util.UUID;

@RestController
@ControllerAdvice
@RequestMapping("/rest/invitations")
public class InvitationsController {

    public static final String IS_ADMIN_CONDITION = "hasRole('tap.admin')";

    public static final String RESEND_INVITATION_URL = "/{email}/resend";

    //:.+ is required, otherwise Spring truncates value with @PathVariable up to last dot
    public static final String DELETE_INVITATION_URL = "/{email:.+}";

    private final InvitationsService invitationsService;

    private final AccessInvitationsService accessInvitationsService;

    private final UserDetailsFinder detailsFinder;

    private final BlacklistEmailValidator emailValidator;

    private final OrgResourceMock orgResourceMock;

    @Autowired
    public InvitationsController(InvitationsService invitationsService,
                                 UserDetailsFinder detailsFinder,
                                 AccessInvitationsService accessInvitationsService,
                                 BlacklistEmailValidator emailValidator,
                                 OrgResourceMock orgResourceMock){
        this.invitationsService = invitationsService;
        this.detailsFinder = detailsFinder;
        this.accessInvitationsService = accessInvitationsService;
        this.emailValidator = emailValidator;
        this.orgResourceMock = orgResourceMock;
    }

    @ApiOperation(
            value = "Add a new invitation for email.",
            notes = "Privilege level: Consumer of this endpoint must have a valid token containing console.admin scope")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "OK", response = InvitationErrorDescription.class),
            @ApiResponse(code = 409, message = "Invalid email format."),
            @ApiResponse(code = 409, message = "User already exists."),
            @ApiResponse(code = 500, message = "Internal server error, e.g. error connecting to CloudController")
    })
    @RequestMapping(method = RequestMethod.POST)
    @PreAuthorize(IS_ADMIN_CONDITION)
    @ResponseStatus(HttpStatus.CREATED)
    public InvitationErrorDescription addInvitation(@RequestBody Invitation invitation,
                                               @ApiParam(hidden = true) Authentication authentication) {

        String userToInviteEmail = invitation.getEmail();
        emailValidator.validate(userToInviteEmail);
        if (invitationsService.userExists(userToInviteEmail)) {
            throw new UserExistsException(String.format("User %s already exists", userToInviteEmail));
        }

        return accessInvitationsService.getAccessInvitations(userToInviteEmail)
                .map(inv -> {
                    accessInvitationsService.updateAccessInvitation(userToInviteEmail, inv);
                    return new InvitationErrorDescription(InvitationErrorDescription.State.UPDATED, "Updated pending invitation");
                }).orElseGet(() -> {
                    String currentUserName = detailsFinder.findUserName(authentication);
                    String invitationLink = invitationsService.sendInviteEmail(userToInviteEmail, currentUserName);
                    UUID orgGuid = UUID.fromString(orgResourceMock.get().getGuid());
                    accessInvitationsService.createOrUpdateInvitation(userToInviteEmail,
                            ui -> ui.addOrgAccessInvitation(orgGuid, UserRole.USER));
                    return new InvitationErrorDescription(InvitationErrorDescription.State.NEW, invitationLink);
                });
    }

    @ApiOperation(
            value = "Get pending invitations.",
            notes = "Privilege level: Consumer of this endpoint must have a valid token containing console.admin scope ")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = String.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Internal server error, e.g. error connecting to CloudController")
    })
    @RequestMapping(method = RequestMethod.GET)
    @PreAuthorize(IS_ADMIN_CONDITION)
    public Set<String> getPendingInvitations() {
        return invitationsService.getPendingInvitationsEmails();
    }

    @ApiOperation(
            value = "Resend invitation to the email.",
            notes = "Privilege level: Consumer of this endpoint must have a valid token containing console.admin scope ")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Invitation not found."),
            @ApiResponse(code = 500, message = "Internal server error, e.g. error connecting to CloudController")
    })
    @RequestMapping(value = RESEND_INVITATION_URL,  method = RequestMethod.POST)
    @PreAuthorize(IS_ADMIN_CONDITION)
    public void resendInvitation(@PathVariable("email") String userToInviteEmail,
                                 @ApiParam(hidden = true) Authentication authentication) {
        String currentUserName = detailsFinder.findUserName(authentication);
        invitationsService.resendInviteEmail(userToInviteEmail, currentUserName);
    }

    @ApiOperation(
            value = "Delete an invitation.",
            notes = "Privilege level: Consumer of this endpoint must have a valid token containing console.admin scope ")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "OK"),
            @ApiResponse(code = 404, message = "Invitation not found."),
            @ApiResponse(code = 500, message = "Internal server error, e.g. error connecting to CloudController")
    })
    @RequestMapping(value = DELETE_INVITATION_URL, method = RequestMethod.DELETE)
    @PreAuthorize(IS_ADMIN_CONDITION)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteInvitation(@PathVariable("email") String email) {
        invitationsService.deleteInvitation(email);
    }


    @ExceptionHandler(InvitationNotSentException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    protected InvitationErrorDescription invitationNotSend(InvitationNotSentException e) {
        return new InvitationErrorDescription(InvitationErrorDescription.State.ERROR, e.getInvContent());
    }
}
