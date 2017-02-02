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
package org.trustedanalytics.usermanagement.invitations.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cloudfoundry.identity.uaa.scim.ScimUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring4.SpringTemplateEngine;
import org.trustedanalytics.uaa.UaaOperations;
import org.trustedanalytics.usermanagement.invitations.NoPendingInvitationFoundException;
import org.trustedanalytics.usermanagement.invitations.UserExistsException;
import org.trustedanalytics.usermanagement.invitations.securitycode.SecurityCode;
import org.trustedanalytics.usermanagement.invitations.securitycode.SecurityCodeService;
import org.trustedanalytics.usermanagement.users.rest.AuthGatewayOperations;

import java.util.Optional;
import java.util.Set;

public class EmailInvitationsService implements InvitationsService {

    private static final Log LOGGER = LogFactory.getLog(EmailInvitationsService.class);

    private final SpringTemplateEngine templateEngine;

    @Autowired
    private MessageService messageService;

    @Autowired
    private SecurityCodeService securityCodeService;

    @Autowired
    private AccessInvitationsService accessInvitationsService;

    @Autowired
    private UaaOperations uaaPrivilegedClient;

    @Autowired
    private InvitationLinkGenerator invitationLinkGenerator;

    @Autowired
    private AuthGatewayOperations authGatewayOperations;

    public EmailInvitationsService(SpringTemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    @Override
    public String sendInviteEmail(String email, String currentUser) {
        SecurityCode sc = securityCodeService.generateCode(email);
        return sendEmail(email, currentUser, sc.getCode());
    }

    @Override
    public String resendInviteEmail(String email, String currentUser) {
        Optional<SecurityCode> sc = securityCodeService.findByMail(email);
        if(!sc.isPresent()) {
            throw new NoPendingInvitationFoundException("No pending invitation for "+email);
        }

        return sendEmail(email, currentUser, sc.get().getCode());
    }

    private String sendEmail(String email, String currentUser, String code) {
        validateUsername(email);
        String subject = "Invitation to join Trusted Analytics platform";
        String invitationLink = invitationLinkGenerator.getLink(code);
        String htmlContent = getEmailHtml(email, currentUser, invitationLink);
        messageService.sendMimeMessage(email, subject, htmlContent);
        LOGGER.info("Sent invitation to user " + email);
        return invitationLink;
    }

    private String getEmailHtml(String email, String currentUser, String invitationLink) {
        final Context ctx = new Context();
        ctx.setVariable("serviceName", "Trusted Analytics");
        ctx.setVariable("email", email);
        ctx.setVariable("currentUser", currentUser);
        ctx.setVariable("accountsUrl", invitationLink);
        return templateEngine.process("invite", ctx);
    }

    @Override
    public Optional<String> createUser(String username, String password, String orgId) {
        validateUsername(username);
        return createAndRetrieveUser(username, password, orgId);
    }

    @Override
    public boolean userExists(String username) {
        return uaaPrivilegedClient.findUserIdByName(username).isPresent();
    }

    @Override
    public Set<String> getPendingInvitationsEmails() {
        return securityCodeService.getKeys();
    }

    @Override
    public void deleteInvitation(String email) {
        Optional<SecurityCode> sc = securityCodeService.findByMail(email);
        if(!sc.isPresent()) {
            throw new NoPendingInvitationFoundException("No pending invitation for "+email);
        }

        securityCodeService.redeem(sc.get());
        accessInvitationsService.redeemAccessInvitations(email);
    }

    private Optional<String> createAndRetrieveUser(String username, String password, String orgId) {
        return accessInvitationsService.getAccessInvitations(username)
                .map(invitations -> {
                    final ScimUser user = uaaPrivilegedClient.createUser(username, password);
                    authGatewayOperations.createUser(orgId, user.getId(), ex ->
                        // rollback adding user to UAA
                        uaaPrivilegedClient.deleteUser(user.getId()));
                    return user.getId();
                });
    }

    private void validateUsername(String username) {
        uaaPrivilegedClient.findUserIdByName(username).ifPresent(user -> {
            throw new UserExistsException(String.format("Username %s is already taken", username));
        });
    }
}
