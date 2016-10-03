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
package org.trustedanalytics.user.invite;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.trustedanalytics.user.common.EmptyPasswordException;
import org.trustedanalytics.user.common.EntityNotFoundException;
import org.trustedanalytics.user.common.TooShortPasswordException;
import org.trustedanalytics.user.common.WrongUserRolesException;
import org.trustedanalytics.user.common.WrongUuidFormatException;
import org.trustedanalytics.user.invite.securitycode.InvalidSecurityCodeException;

import java.io.IOException;

@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RestErrorHandler {

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(InvalidSecurityCodeException.class)
    public void incorrectSocurityCode() {
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(UserExistsException.class)
    public UserConflictResponse userExists(UserExistsException e) throws IOException {
        return UserConflictResponse.of(UserConflictResponse.ConflictedField.USER , e.getMessage());
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(OrgExistsException.class)
    public UserConflictResponse orgExists(OrgExistsException e) throws IOException {
        return UserConflictResponse.of(UserConflictResponse.ConflictedField.ORG, e.getMessage());
    }


    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(InvalidOrganizationNameException.class)
    public String invalidOrgName(InvalidOrganizationNameException e) throws IOException {
        return e.getMessage();
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(WrongUuidFormatException.class)
    public String invalidUuidString(WrongUuidFormatException e) throws IOException {
        return e.getMessage();
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(EntityNotFoundException.class)
    public String entityNotFound(Exception e) throws IOException {
        return e.getMessage();
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(WrongUserRolesException.class)
    public String incorrectRoles(Exception e) throws IOException {
        return e.getMessage();
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(WrongEmailAddressException.class)
    public String wrongEmailAddress(Exception e) throws IOException {
        return e.getMessage();
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(EmptyPasswordException.class)
    public String emptyPassword(Exception e) throws IOException {
        return e.getMessage();
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(TooShortPasswordException.class)
    public String tooShortPassword(Exception e) throws IOException {
        return e.getMessage();
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NoPendingInvitationFoundException.class)
    public String noPendingInvitation(NoPendingInvitationFoundException e)
            throws IOException {
        return e.getMessage();
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(AccessDeniedException.class)
    public String accessDenied(Exception e) throws IOException {
        return e.getMessage();
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public String userExists(HttpRequestMethodNotSupportedException e) throws IOException {
        return e.getMessage();
    }
}
