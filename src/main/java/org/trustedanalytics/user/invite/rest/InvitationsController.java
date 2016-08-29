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
package org.trustedanalytics.user.invite.rest;


import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.trustedanalytics.user.common.BlacklistEmailValidator;
import org.trustedanalytics.user.current.UserDetailsFinder;
import org.trustedanalytics.user.invite.InvitationNotSentException;
import org.trustedanalytics.user.invite.InvitationsService;
import org.trustedanalytics.user.invite.UserExistsException;
import org.trustedanalytics.user.invite.access.AccessInvitationsService;
import org.trustedanalytics.user.mocks.OrganizationResourceMock;
import org.trustedanalytics.user.model.UserRole;

import java.util.Set;
import java.util.UUID;

@RestController
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

    @Autowired
    public InvitationsController(InvitationsService invitationsService,
                                 UserDetailsFinder detailsFinder,
                                 AccessInvitationsService accessInvitationsService,
                                 BlacklistEmailValidator emailValidator){
        this.invitationsService = invitationsService;
        this.detailsFinder = detailsFinder;
        this.accessInvitationsService = accessInvitationsService;
        this.emailValidator = emailValidator;
    }

    @ApiOperation(
            value = "Add a new invitation for email.",
            notes = "Privilege level: Consumer of this endpoint must have a valid token containing console.admin scope")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = ErrorDescriptionModel.class),
            @ApiResponse(code = 409, message = "Invalid email format."),
            @ApiResponse(code = 409, message = "User already exists."),
            @ApiResponse(code = 500, message = "Internal server error, e.g. error connecting to CloudController")
    })
    @RequestMapping(method = RequestMethod.POST)
    @PreAuthorize(IS_ADMIN_CONDITION)
    public ErrorDescriptionModel addInvitation(@RequestBody InvitationModel invitation,
                                               @ApiParam(hidden = true) Authentication authentication) {

        String userToInviteEmail = invitation.getEmail();
        emailValidator.validate(userToInviteEmail);
        if (invitationsService.userExists(userToInviteEmail)) {
            throw new UserExistsException(String.format("User %s already exists", userToInviteEmail));
        }

        return accessInvitationsService.getAccessInvitations(userToInviteEmail)
                .map(inv -> {
                    accessInvitationsService.updateAccessInvitation(userToInviteEmail, inv);
                    return new ErrorDescriptionModel(ErrorDescriptionModel.State.UPDATED, "Updated pending invitation");
                }).orElseGet(() -> {
                    String currentUserName = detailsFinder.findUserName(authentication);
                    String invitationLink = invitationsService.sendInviteEmail(userToInviteEmail, currentUserName);
                    UUID orgGuid = OrganizationResourceMock.get().getGuid();
                    accessInvitationsService.createOrUpdateInvitation(userToInviteEmail,
                            ui -> ui.addOrgAccessInvitation(orgGuid, UserRole.USER));
                    return new ErrorDescriptionModel(ErrorDescriptionModel.State.NEW, invitationLink);
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
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Invitation not found."),
            @ApiResponse(code = 500, message = "Internal server error, e.g. error connecting to CloudController")
    })
    @RequestMapping(value = DELETE_INVITATION_URL, method = RequestMethod.DELETE)
    @PreAuthorize(IS_ADMIN_CONDITION)
    public void deleteInvitation(@PathVariable("email") String email) {
        invitationsService.deleteInvitation(email);
    }


    @ExceptionHandler(InvitationNotSentException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    protected ErrorDescriptionModel invitationNotSend(InvitationNotSentException e) {
        return new ErrorDescriptionModel(ErrorDescriptionModel.State.ERROR, e.getInvContent());
    }
}
