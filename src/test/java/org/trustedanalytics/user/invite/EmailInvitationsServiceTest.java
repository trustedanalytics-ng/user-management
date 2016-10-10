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
package org.trustedanalytics.user.invite;

import org.cloudfoundry.identity.uaa.scim.ScimUser;
import org.junit.Before;
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
import org.trustedanalytics.user.invite.access.AccessInvitations;
import org.trustedanalytics.user.invite.access.AccessInvitationsService;
import org.trustedanalytics.user.invite.keyvaluestore.KeyValueStore;
import org.trustedanalytics.user.invite.securitycode.SecurityCodeService;
import org.trustedanalytics.user.manageusers.AuthGatewayOperations;
import org.trustedanalytics.user.manageusers.UserState;
import org.trustedanalytics.user.mocks.OrganizationResourceMock;
import org.trustedanalytics.user.model.UserRole;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {EmailInvitationsServiceTest.EmailInvitationsServiceTestConfiguration.class})
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

    @Configuration
    @Profile("unit-test")
    public static class EmailInvitationsServiceTestConfiguration {
        @Bean
        public AccessInvitationsService accessInvitationsService() {
            final KeyValueStore<AccessInvitations> keyValueStore = mock(KeyValueStore.class);
            final AccessInvitationsService service =  new AccessInvitationsService(keyValueStore);
            AccessInvitations accessInvitations = new AccessInvitations();
            accessInvitations.addOrgAccessInvitation(UUID.randomUUID(), UserRole.USER);
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
            when(authGatewayOperations.createUser(OrganizationResourceMock.get().getGuid().toString(), USER_ID)).thenReturn(new UserState(SAMPLE_EMAIL_ADDRESS, USER_ID, true));
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
        inOrder.verify(authGatewayOperations).createUser(OrganizationResourceMock.get().getGuid().toString(), userGuid.get().toString());
    }

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void createUser_userExists_throwUserExistException() {

        exception.expect(UserExistsException.class);
        sut.createUser(OTHER_SAMPLE_EMAIL_ADDRESS, PASSWORD);

    }
}
