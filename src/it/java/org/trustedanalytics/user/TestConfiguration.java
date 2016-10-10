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
package org.trustedanalytics.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;
import org.thymeleaf.spring4.SpringTemplateEngine;
import org.trustedanalytics.auth.AuthTokenRetriever;
import org.trustedanalytics.uaa.UaaOperations;
import org.trustedanalytics.user.common.BlacklistEmailValidator;
import org.trustedanalytics.user.common.FormatUserRolesValidator;
import org.trustedanalytics.user.common.OAuth2PrivilegedInterceptor;
import org.trustedanalytics.user.common.UserPasswordValidator;
import org.trustedanalytics.user.current.UserDetailsFinder;
import org.trustedanalytics.user.invite.EmailInvitationsService;
import org.trustedanalytics.user.invite.EmailService;
import org.trustedanalytics.user.invite.InvitationLinkGenerator;
import org.trustedanalytics.user.invite.InvitationsService;
import org.trustedanalytics.user.invite.MessageService;
import org.trustedanalytics.user.invite.SecurityDisabler;
import org.trustedanalytics.user.invite.access.AccessInvitations;
import org.trustedanalytics.user.invite.access.AccessInvitationsService;
import org.trustedanalytics.user.manageusers.AuthGatewayOperations;
import org.trustedanalytics.user.manageusers.UsersService;
import org.trustedanalytics.user.mocks.OrganizationResourceMock;

import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;

import static org.mockito.Mockito.mock;

@Configuration
public class TestConfiguration {

    @Value("${smtp.email}")
    private String SUPPORT_EMAIL;

    @Value("${smtp.email_name}")
    private String EMAIL_NAME;

    @Bean
    protected UsersService usersService() {
        return mock(UsersService.class);
    }

    @Bean
    protected RestOperations userRestTemplate() {
        return mock(RestTemplate.class);
    }

    @Bean
    protected UaaOperations uaaClient() {
        return mock(UaaOperations.class);
    }

    @Bean
    protected InvitationsService invitationsService(SpringTemplateEngine mailTemplateEngine) {
        return new EmailInvitationsService(mailTemplateEngine);
    }

    @Bean
    protected String TOKEN() {
        //we can use any string as a token, because all the rest endpoints have been ignored for the test purposes in src/test/application.yml
        return "jhksdf8723kjhdfsh4i187y91hkajl";
    }

    @Bean
    protected AccessInvitationsService accessInvitationsService() {
        return mock(AccessInvitationsService.class);
    }

    @Bean
    protected AuthTokenRetriever tokenRetriever() {
        return mock(AuthTokenRetriever.class);
    }
    
    @Bean
    protected UserDetailsFinder detailsFinder() {
        return mock(UserDetailsFinder.class);
    }

    @Bean
    public MimeMessage mimeMessage(){
        return mock(MimeMessage.class);
    }

    @Bean
    public JavaMailSender mailSender(){
        return mock(JavaMailSender.class);
    }

    @Bean
    public MessageService service() throws UnsupportedEncodingException{
        return new EmailService(mailSender(), SUPPORT_EMAIL, EMAIL_NAME);
    }

    @Bean
    public AuthGatewayOperations authGatewayOperations() {
        return mock(AuthGatewayOperations.class);
    }

    @Bean
    public OAuth2PrivilegedInterceptor oAuth2PrivilegedInterceptor() {
        return mock(OAuth2PrivilegedInterceptor.class);
    }

    @Bean
    public BeanPostProcessor securityDisabler(){
        return new SecurityDisabler();
    }

    @Bean
    protected BlacklistEmailValidator emailValidator() {
        return mock(BlacklistEmailValidator.class);
    }

    @Bean
    protected FormatUserRolesValidator formatRolesValidator() {
        return mock(FormatUserRolesValidator.class);
    }

    @Bean
    protected UserPasswordValidator passwordValidator() {
        return mock(UserPasswordValidator.class);
    }

    @Bean
    protected AccessInvitations accessInvitations() {
        return mock(AccessInvitations.class);
    }

    @Bean
    protected InvitationLinkGenerator invitationLinkGenerator() {
        return mock(InvitationLinkGenerator.class);
    }

    @Bean
    protected OrganizationResourceMock organizationResourceMock() {
        return new OrganizationResourceMock();
    }
}
