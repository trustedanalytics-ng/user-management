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

package org.trustedanalytics.user.mocks;

import com.google.common.collect.ImmutableList;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.trustedanalytics.user.model.Org;

import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

// TODO: remove this class after integration with TAP NG
@Configuration
public class OrganizationResourceMock {

    private static final Org ORGANIZATION_MOCK =
            new Org(UUID.fromString("00000000-0000-0000-0000-000000000000"), "default");

    private static final Collection<Org> ORGANIZATIONS = Arrays.asList(ORGANIZATION_MOCK);

    @Bean
    public Collection<Org> getOrganizations() {
        return ImmutableList.copyOf(ORGANIZATIONS);
    }

    public static Org get() {
        return ORGANIZATION_MOCK;
    }
}
