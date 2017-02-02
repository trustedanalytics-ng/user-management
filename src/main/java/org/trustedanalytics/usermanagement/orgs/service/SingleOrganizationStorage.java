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

package org.trustedanalytics.usermanagement.orgs.service;

import com.google.common.collect.Lists;
import org.trustedanalytics.usermanagement.orgs.model.Org;

import java.util.Collection;
import java.util.Optional;

import static java.util.Collections.singleton;

// TODO: use this class only in single-organization mode
public class SingleOrganizationStorage implements OrganizationsStorage {

    private final Org org;
    private final Collection<Org> orgList;

    public SingleOrganizationStorage(String id, String name) {
        org = new Org(id, name);
        orgList = singleton(org);
    }

    @Override
    public Collection<Org> getOrganizations() {
        return Lists.newArrayList(orgList);
    }

    @Override
    public Optional<Org> getOrganization(String orgId) {
        return orgId.equals(org.getGuid()) ? Optional.of(org) : Optional.<Org>empty();
    }
}
