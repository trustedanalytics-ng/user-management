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
package org.trustedanalytics.usermanagement.orgs.rest;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.trustedanalytics.usermanagement.orgs.mocks.OrgResourceMock;
import org.trustedanalytics.usermanagement.orgs.model.Org;
import org.trustedanalytics.usermanagement.orgs.model.OrgNameRequest;
import org.trustedanalytics.usermanagement.security.service.UserDetailsFinder;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.Collection;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

@RestController
public class OrgsController {
    public static final String GENERAL_ORGS_URL = "/rest/orgs";

    private final UserDetailsFinder detailsFinder;
    private final OrgResourceMock orgResourceMock;

    @Autowired
    public OrgsController(UserDetailsFinder detailsFinder, OrgResourceMock orgResourceMock) {
        this.detailsFinder = detailsFinder;
        this.orgResourceMock = orgResourceMock;
    }

    @ApiOperation(
            value = "Returns list containing one organization",
            notes = "Privilege level: Any consumer of this endpoint must have a valid access token"
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = Org.class, responseContainer = "List"),
    })
    @RequestMapping(value = GENERAL_ORGS_URL, method = GET,
        produces = APPLICATION_JSON_VALUE)
    public Collection<Org> getOrgs(@ApiParam(hidden = true) Authentication auth) {
        return orgResourceMock.getOrganizations();
    }

    @ApiOperation(value = "Renaming organization is not supported in this application version")
    @ApiResponses(value = {
            @ApiResponse(code = 501, message = "Renaming organization is not supported in this application version")
    })
    @RequestMapping(value = GENERAL_ORGS_URL + "/{org}/name", method = PUT, consumes = APPLICATION_JSON_VALUE)
    public void renameOrg(@RequestBody OrgNameRequest request, @PathVariable String org) {
        throw new NotImplementedException();
    }

    //TODO send requests to auth-gateway in case of creating or removing organization
    
    @ApiOperation(value = "Deleting organization is not supported in this application version")
    @ApiResponses(value = {
            @ApiResponse(code = 501, message = "Deleting organization is not supported in this application version")
    })
    @RequestMapping(value = GENERAL_ORGS_URL + "/{org}", method = DELETE)
    public void deleteOrg(@PathVariable String org) {
        throw new NotImplementedException();
    }

    @ApiOperation(value = "Creating organization is not supported in this application version")
    @ApiResponses(value = {
            @ApiResponse(code = 501, message = "Creating organization is not supported in this application version")
    })
    @RequestMapping(value = GENERAL_ORGS_URL, method = POST, consumes = APPLICATION_JSON_VALUE)
    public String createOrg(@RequestBody OrgNameRequest request) {
        throw new NotImplementedException();
    }
}
