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
package org.trustedanalytics.usermanagement.users.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;
import org.trustedanalytics.auth.AuthTokenRetriever;
import org.trustedanalytics.auth.HeaderAddingHttpInterceptor;
import org.trustedanalytics.uaa.UaaClient;
import org.trustedanalytics.uaa.UaaOperations;
import org.trustedanalytics.usermanagement.invitations.service.AccessInvitationsService;
import org.trustedanalytics.usermanagement.invitations.service.InvitationsService;
import org.trustedanalytics.usermanagement.security.OAuth2PrivilegedInterceptor;
import org.trustedanalytics.usermanagement.users.PasswordGenerator;
import org.trustedanalytics.usermanagement.users.RandomPasswordGenerator;
import org.trustedanalytics.usermanagement.users.rest.AuthGatewayOperations;
import org.trustedanalytics.usermanagement.users.service.UaaUsersService;
import org.trustedanalytics.usermanagement.users.service.UsersService;

import static java.util.Collections.singletonList;
import static org.springframework.context.annotation.ScopedProxyMode.TARGET_CLASS;
import static org.springframework.web.context.WebApplicationContext.SCOPE_REQUEST;

@Profile("cloud")
@Configuration
public class UsersServiceConfig {
    @Value("${oauth.uaa}")
    private String uaaBaseUrl;

    @Value("${oauth.resource}")
    private String apiBaseUrl;

    @Autowired
    private AuthTokenRetriever tokenRetriever;

    @Bean
    protected OAuth2PrivilegedInterceptor oauth2PrivilegedInterceptor(OAuth2ProtectedResourceDetails clientCredentials) {
        return new OAuth2PrivilegedInterceptor(clientCredentials);
    }

    @Bean
    protected UaaOperations uaaPrivilegedClient(RestOperations clientRestTemplate) {
        return new UaaClient(uaaBaseUrl, clientRestTemplate);
    }

    @Bean
    @Scope(value = SCOPE_REQUEST, proxyMode = TARGET_CLASS)
    protected UaaOperations uaaClient(RestTemplate userRestTemplate) {
        return new UaaClient(uaaBaseUrl, setAccessToken(userRestTemplate));
    }

    private OAuth2Authentication getAuthentication() {
        SecurityContext context = SecurityContextHolder.getContext();
        if (context == null) {
            return null;
        }

        return (OAuth2Authentication) context.getAuthentication();
    }

    private RestTemplate setAccessToken(RestTemplate restTemplate) {
        ClientHttpRequestInterceptor interceptor =
                new HeaderAddingHttpInterceptor("Authorization", "bearer " + getAccessToken());
        restTemplate.setInterceptors(singletonList(interceptor));

        return restTemplate;
    }

    private String getAccessToken() {
        OAuth2Authentication authentication = getAuthentication();
        return tokenRetriever.getAuthToken(authentication);
    }

    @Bean
    protected UsersService usersService(UaaOperations uaaClient,
                                        InvitationsService invitationsService,
                                        AccessInvitationsService accessInvitationsService,
                                        AuthGatewayOperations authGatewayOperations) {
        return new UaaUsersService(uaaClient, invitationsService, accessInvitationsService, authGatewayOperations);
    }

    @Bean
    protected UsersService priviledgedUsersService(UaaOperations uaaPrivilegedClient,
                                                   InvitationsService invitationsService,
                                                   AccessInvitationsService accessInvitationsService,
                                                   AuthGatewayOperations authGatewayOperations) {
        return new UaaUsersService(
                uaaPrivilegedClient,
                invitationsService,
                accessInvitationsService,
                authGatewayOperations);
    }


    @Bean
    protected PasswordGenerator passwordGenerator() {
        return new RandomPasswordGenerator();
    }
}
