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

package org.trustedanalytics.user.common;

import org.trustedanalytics.user.invite.WrongEmailAddressException;

import java.util.List;
import java.util.regex.Pattern;

public class BlacklistEmailValidator implements EmailValidator {
    private static final String EMAIL_PATTERN = "^[a-z0-9!#$%&'*+=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+=?^_`{|}~-]+)*@" +
            "(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?$";

    private static final int MAX_NUMBER_OF_CHARACTERS_IN_DOMAIN_PART = 252;
    private static final int MAX_NUMBER_OF_CHARACTERS_IN_LOCAL_PART = 64;

    private final List<String> forbiddenDomains;

    public BlacklistEmailValidator(List<String> forbiddenDomains) {
        this.forbiddenDomains = forbiddenDomains;
    }

    private void validateDomain(String email) {
        if(forbiddenDomains.contains(getDomainPart(email))){
            throw new WrongEmailAddressException("That domain is blocked");
        }
        if(getDomainPart(email).length() > MAX_NUMBER_OF_CHARACTERS_IN_DOMAIN_PART) {
            throw new WrongEmailAddressException("Domain part of email address is too long");
        }
    }

    private void validateEmailAddress(String email) {
        Pattern pattern = Pattern.compile(EMAIL_PATTERN);
        if(!pattern.matcher(email).matches()) {
            throw new WrongEmailAddressException("That email address is not valid");
        }
        if(getLocalPart(email).length() > MAX_NUMBER_OF_CHARACTERS_IN_LOCAL_PART) {
            throw new WrongEmailAddressException("Local part of email address is too long");
        }
    }

    /* That method at first checks whether string passed into parameter
     * conforms to the syntax rules of RFC 822. Second method checks
     * whether email address is on a blacklist defined in application.yml
     */
    @Override
    public void validate(String email) {
        validateEmailAddress(email);
        validateDomain(email);
    }

    private String getDomainPart(String email) {
        return email.substring(email.indexOf("@") + 1).toLowerCase();
    }

    private String getLocalPart(String email) {
        return email.substring(0, email.indexOf("@")).toLowerCase();
    }
}