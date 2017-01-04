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
package org.trustedanalytics.usermanagement.invitations;

import org.cloudfoundry.identity.uaa.scim.ScimUser;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.thymeleaf.spring4.SpringTemplateEngine;
import org.trustedanalytics.uaa.UaaOperations;
import org.trustedanalytics.uaa.UserIdNamePair;
import org.trustedanalytics.usermanagement.invitations.service.InvitationLinkGenerator;
import org.trustedanalytics.usermanagement.invitations.service.AccessInvitations;
import org.trustedanalytics.usermanagement.invitations.securitycode.SecurityCodeService;
import org.trustedanalytics.usermanagement.invitations.service.AccessInvitationsService;
import org.trustedanalytics.usermanagement.invitations.service.EmailInvitationsService;
import org.trustedanalytics.usermanagement.invitations.service.Fallback;
import org.trustedanalytics.usermanagement.invitations.service.InvitationsService;
import org.trustedanalytics.usermanagement.invitations.service.MessageService;
import org.trustedanalytics.usermanagement.orgs.config.OrgConfig;
import org.trustedanalytics.usermanagement.orgs.mocks.OrgResourceMock;
import org.trustedanalytics.usermanagement.orgs.model.Org;
import org.trustedanalytics.usermanagement.storage.KeyValueStore;
import org.trustedanalytics.usermanagement.users.model.UserRole;
import org.trustedanalytics.usermanagement.users.model.UserState;
import org.trustedanalytics.usermanagement.users.rest.AuthGatewayOperations;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {EmailInvitationsServiceTest.EmailInvitationsServiceTestConfiguration.class, OrgConfig.class})
@ActiveProfiles("unit-test")
public class EmailInvitationsServiceTest {

    private static final String USER_ID = "00012345-2345-2345-2345-000000012345";
    private static final String SAMPLE_EMAIL_ADDRESS = "sampleuser@example.com";
    private static final String OTHER_SAMPLE_EMAIL_ADDRESS = "otheruser@example.com";
    private static final String PASSWORD = "password";

    @Autowired
    private InvitationsService sut;

    @Autowired
    private UaaOperations uaaPrivilegedClient;

    @Autowired
    private AuthGatewayOperations authGatewayOperations;

    @Autowired
    private OrgResourceMock orgResourceMock;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Configuration
    @Profile("unit-test")
    public static class EmailInvitationsServiceTestConfiguration {

        @Autowired
        private OrgResourceMock orgResourceMock;

        @Bean
        public AccessInvitationsService accessInvitationsService() {
            final KeyValueStore<AccessInvitations> keyValueStore = mock(KeyValueStore.class);
            final AccessInvitationsService service =  new AccessInvitationsService(keyValueStore);
            AccessInvitations accessInvitations = new AccessInvitations();
            accessInvitations.addOrgAccessInvitation(orgResourceMock.get().getGuid(), UserRole.USER);
            when(keyValueStore.get(SAMPLE_EMAIL_ADDRESS)).thenReturn(accessInvitations);
            return service;
        }

        @Bean
        public SpringTemplateEngine springTemplateEngine() {
            return mock(SpringTemplateEngine.class);
        }

        @Bean
        public UaaOperations uaaPrivilegedClient() {
            final UaaOperations uaaOperations = mock(UaaOperations.class);
            when(uaaOperations.findUserIdByName(SAMPLE_EMAIL_ADDRESS)).thenReturn(Optional.<UserIdNamePair>empty());
            when(uaaOperations.findUserIdByName(OTHER_SAMPLE_EMAIL_ADDRESS)).thenReturn(Optional.of(UserIdNamePair.of(UUID.randomUUID(), OTHER_SAMPLE_EMAIL_ADDRESS)));
            when(uaaOperations.createUser(SAMPLE_EMAIL_ADDRESS, PASSWORD)).thenReturn(new ScimUser(USER_ID, SAMPLE_EMAIL_ADDRESS, null, null));
            when(uaaOperations.createUser(OTHER_SAMPLE_EMAIL_ADDRESS, PASSWORD)).thenReturn(new ScimUser(USER_ID, SAMPLE_EMAIL_ADDRESS, null, null));
            return uaaOperations;
        }

        @Bean
        public AuthGatewayOperations authGatewayOperations() {
            final AuthGatewayOperations authGatewayOperations = mock(AuthGatewayOperations.class);
            final String orgId = orgResourceMock.get().getGuid();
            when(authGatewayOperations.createUser(orgId, USER_ID, mock(Fallback.class))).thenReturn(new UserState(SAMPLE_EMAIL_ADDRESS, USER_ID, true));
            return authGatewayOperations;
        }

        @Bean
        public KeyValueStore keyValueStore() {
            return mock(KeyValueStore.class);
        }

        @Bean
        public EmailInvitationsService emailInvitationsService() {
            return new EmailInvitationsService(springTemplateEngine());
        }

        @Bean
        public MessageService messageService() {
            return mock(MessageService.class);
        }

        @Bean
        public SecurityCodeService securityCodeService() {
            return mock(SecurityCodeService.class);
        }

        @Bean
        public InvitationLinkGenerator invitationLinkGenerator() {
            return mock(InvitationLinkGenerator.class);
        }
    }

    @Test
    public void createUser_userDoesNotExist_sendRequestToUaaAndAuthGateway() {
        // when
        final Optional<UUID> userGuid = sut.createUser(SAMPLE_EMAIL_ADDRESS, PASSWORD);

        // then
        final InOrder inOrder = inOrder(uaaPrivilegedClient, authGatewayOperations);

        inOrder.verify(uaaPrivilegedClient).createUser(SAMPLE_EMAIL_ADDRESS, PASSWORD);
        inOrder.verify(authGatewayOperations).createUser(eq(orgResourceMock.get().getGuid()), eq(userGuid.get().toString()), any());
    }

    @Test
    public void createUser_userExists_throwUserExistException() {
        exception.expect(UserExistsException.class);

        sut.createUser(OTHER_SAMPLE_EMAIL_ADDRESS, PASSWORD);
    }
}
