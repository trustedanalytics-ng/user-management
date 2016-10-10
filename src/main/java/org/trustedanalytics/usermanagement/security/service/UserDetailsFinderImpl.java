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
package org.trustedanalytics.usermanagement.security.service;

import com.google.common.base.Preconditions;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.trustedanalytics.usermanagement.security.AccessTokenDetails;
import org.trustedanalytics.usermanagement.users.model.UserRole;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public final class UserDetailsFinderImpl implements UserDetailsFinder {

    public static final String ADMIN_GROUP = "tap.admin";

    @Override
    public UserRole findUserRole(Authentication authentication) {
        verifyAuthenticationToken(authentication);
        Optional<Collection<String>> scope = Optional.ofNullable(retreiveScope(authentication));
        return isAdmin(scope) ? UserRole.ADMIN : UserRole.USER;
    }

    @Override
    public UUID findUserId(Authentication authentication) {
        verifyAuthenticationToken(authentication);
        OAuth2Authentication oauth2 = (OAuth2Authentication) authentication;
        AccessTokenDetails details = (AccessTokenDetails) oauth2.getUserAuthentication().getDetails();
        return details.getUserGuid();
    }

    @Override
    public String findUserName(Authentication authentication) {
        verifyAuthenticationToken(authentication);
        OAuth2Authentication oauth2 = (OAuth2Authentication) authentication;
        return oauth2.getName();
    }

    private void verifyAuthenticationToken(Authentication authentication) {
        Preconditions.checkNotNull(authentication, "Authentication argument must not be null");
    }

    private Collection<String> retreiveScope(Authentication authentication) {
        OAuth2Authentication oAuth2Authentication = (OAuth2Authentication) authentication;
        OAuth2Request oAuth2Request = oAuth2Authentication.getOAuth2Request();
        return oAuth2Request.getScope();
    }

    private boolean isAdmin(Optional<Collection<String>> scope) {
        return scope.map(s -> s.contains(ADMIN_GROUP)).orElse(false);
    }
}
