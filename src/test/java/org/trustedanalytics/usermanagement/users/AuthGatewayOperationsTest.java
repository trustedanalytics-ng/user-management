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
package org.trustedanalytics.usermanagement.users;

import feign.Feign;
import feign.Request;
import feign.Retryer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.trustedanalytics.usermanagement.users.rest.AuthGatewayOperations;

public class AuthGatewayOperationsTest {

    private static final String FALLBACK_OCCURRED_MESSAGE = "fallback occurred";

    private static final AuthGatewayOperations AUTH_GATEWAY_OPERATIONS =
        Feign.builder()
            .options(new Request.Options(0, 0))
            .retryer(new Retryer.Default(0, 0, 0))
            .target(AuthGatewayOperations.class, "http://localhost");

    @Rule
    public ExpectedException expectedException = ExpectedException.none();


    @Test
    public void testCreateUserFallbackOccursOnFailedRequest() {
        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage(FALLBACK_OCCURRED_MESSAGE);

        AUTH_GATEWAY_OPERATIONS.createUser("orgId", "userId", ex -> {
            throw new IllegalStateException(FALLBACK_OCCURRED_MESSAGE);
        });
    }

    @Test
    public void testDeleteUserFallbackOccursOnFailedRequest() {
        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage(FALLBACK_OCCURRED_MESSAGE);

        AUTH_GATEWAY_OPERATIONS.deleteUser("orgId", "userId", ex -> {
            throw new IllegalStateException(FALLBACK_OCCURRED_MESSAGE);
        });
    }

    @Test
    public void testCreateUserFallbackHandledRequest() {
        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("unable to add user userId to cdh");

        AUTH_GATEWAY_OPERATIONS.createUser("orgId", "userId", ex -> {
            // ignore
        });
    }

    @Test
    public void testDeleteUserFallbackHandledRequest() {
        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("unable to delete user userId from cdh");

        AUTH_GATEWAY_OPERATIONS.deleteUser("orgId", "userId", ex -> {
            // ignore
        });
    }

}
