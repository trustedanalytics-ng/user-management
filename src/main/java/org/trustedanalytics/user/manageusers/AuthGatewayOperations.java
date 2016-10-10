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

    @RequestLine("DELETE /organizations/{orgId}/users/{userId}")
    UserState deleteUser(@Param("orgId") String orgId, @Param("userId") String userId);

}
