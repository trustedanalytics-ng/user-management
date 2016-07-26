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
package org.trustedanalytics.users.orgs;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.trustedanalytics.user.current.UserDetailsFinder;
import org.trustedanalytics.user.invite.config.AccessTokenDetails;
import org.trustedanalytics.user.manageusers.OrgNameRequest;
import org.trustedanalytics.user.model.Org;
import org.trustedanalytics.user.orgs.OrgsController;
import org.trustedanalytics.user.mocks.OrganizationResourceMock;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OrgsControllerTest {

    private OrgsController sut;
    private Org existingOrganization = new Org(UUID.randomUUID(), "the-only-org");

    @Mock
    private OrganizationResourceMock organizationResource;

    @Mock
    private Authentication userAuthentication;

    @Mock
    private UserDetailsFinder detailsFinder;

    @Before
    public void Setup() {
        sut = new OrgsController(detailsFinder, organizationResource);
        when(organizationResource.getOrganizations()).thenReturn(Arrays.asList(existingOrganization));
    }

    @Test
    public void getOrgs_returnOneOrganization() {
        Collection<Org> expectedOrgs = Arrays.asList(existingOrganization);
        AccessTokenDetails details = new AccessTokenDetails(UUID.randomUUID());
        when(userAuthentication.getDetails()).thenReturn(details);
        OAuth2Authentication auth = new OAuth2Authentication(null, userAuthentication);

        Collection<Org> orgs = sut.getOrgs(auth);

        assertEquals(expectedOrgs, orgs);
    }

    @Test(expected = NotImplementedException.class)
    public void renameOrg() {
        UUID orgId = UUID.randomUUID();
        String testName = "test-name";
        OrgNameRequest request = new OrgNameRequest();
        request.setName(testName);

        sut.renameOrg(request, orgId.toString());
    }

    @Test(expected = NotImplementedException.class)
    public void deleteOrg() {
        UUID orgId = UUID.randomUUID();

        sut.deleteOrg(orgId.toString());
    }

    @Test(expected = NotImplementedException.class)
    public void createOrg() {
        final String orgName = "test-name";
        final OrgNameRequest request = new OrgNameRequest();
        request.setName(orgName);

        sut.createOrg(request);
    }
}
