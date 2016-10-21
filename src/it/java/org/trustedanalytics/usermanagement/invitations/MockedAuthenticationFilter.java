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
package org.trustedanalytics.usermanagement.invitations;

import com.google.common.collect.ImmutableList;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.security.Principal;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class MockedAuthenticationFilter extends BasicAuthenticationFilter {

    public MockedAuthenticationFilter() {
        super(authentication -> null);
    }

    @Override public void doFilter(ServletRequest request, ServletResponse response,
        FilterChain chain) throws IOException, ServletException {

        Authentication authenticationMock = mock(Authentication.class);

        doReturn(ImmutableList.of((GrantedAuthority) () -> "tap.admin"))
            .when(authenticationMock).getAuthorities();
        doReturn((Principal) () -> "admin")
            .when(authenticationMock).getPrincipal();
        doReturn(true)
            .when(authenticationMock).isAuthenticated();
        doReturn("admin")
            .when(authenticationMock).getName();

        SecurityContextHolder.getContext().setAuthentication(authenticationMock);

        chain.doFilter(request, response);
    }
}
