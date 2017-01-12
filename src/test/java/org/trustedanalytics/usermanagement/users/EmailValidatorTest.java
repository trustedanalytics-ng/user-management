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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.trustedanalytics.usermanagement.invitations.WrongEmailAddressException;

import java.util.ArrayList;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)

public class EmailValidatorTest {

    private static final String VALID_EMAIL_GOOD_DOMAIN = "foo@examplee.com";
    private static final String INVALID_EMAIL_CAPITAL_LETTERS = "FOO@EXAMPLEE.COM";
    private static final String VALID_EMAIL_PLUS = "foo+fop2@examplee.com";
    private static final String VALID_EMAIL_BLOCKED_DOMAIN = "foo@example.com";
    private static final String INVALID_EMAIL = ".foo@bar@.";
    private BlacklistEmailValidator emailValidator;

    private List<String> forbiddenDomains = new ArrayList<>();

    @Before
    public void setUp(){
        forbiddenDomains.add("example.com");
        emailValidator = new BlacklistEmailValidator(forbiddenDomains);
    }


    @Test(expected = WrongEmailAddressException.class)
    public void validateEmailAddress_blockedDomain_exceptionThrown(){
        emailValidator.validate(VALID_EMAIL_BLOCKED_DOMAIN);
    }

    @Test
    public void validateEmailAddress_goodDomain(){
        emailValidator.validate(VALID_EMAIL_GOOD_DOMAIN);
    }

    @Test
    public void validateEmailAddress_plus(){
        emailValidator.validate(VALID_EMAIL_PLUS);
    }

    @Test(expected = WrongEmailAddressException.class)
    public void validateEmailAddress_capitalLetters(){
        emailValidator.validate(INVALID_EMAIL_CAPITAL_LETTERS);
    }

    @Test(expected = WrongEmailAddressException.class)
    public void validateEmailAddress_invalidEmail_exceptionThrown(){
        emailValidator.validate(INVALID_EMAIL);
    }
}