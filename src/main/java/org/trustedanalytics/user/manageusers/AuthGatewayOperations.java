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
package org.trustedanalytics.user.manageusers;

import feign.Headers;
import feign.Param;
import feign.RequestLine;

@Headers("Accept: application/json")
public interface AuthGatewayOperations {

    @RequestLine("PUT /organizations/{orgId}/users/{userId}")
    UserState createUser(@Param("orgId") String orgId, @Param("userId") String userId);

    default UserState createUser(String orgId, String userId, Fallback fallback) {
        try {
            return createUser(orgId, userId);
        } catch (Throwable ex) {
            fallback.accept(ex);
            throw new IllegalStateException(String.format("unable to add user %s to cdh", userId), ex);
        }
    }

    @RequestLine("DELETE /organizations/{orgId}/users/{userId}")
    UserState deleteUser(@Param("orgId") String orgId, @Param("userId") String userId);

    default UserState deleteUser(String orgId, String userId, Fallback fallback) {
        try {
            return deleteUser(orgId, userId);
        } catch (Throwable ex) {
            fallback.accept(ex);
            throw new IllegalStateException(String.format("unable to delete user %s from cdh", userId), ex);
        }
    }

}
