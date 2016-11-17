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
package org.trustedanalytics.usermanagement.common;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.UUID;

public class StringToUuidConverterTest {

    @Test
    public void test_convert_proper_string_to_UUID() {
        UUID testGuid = UUID.randomUUID();
        String testGuidString = testGuid.toString();
        UUID resultGuid = StringToUuidConverter.convert(testGuidString);
        
        Assert.assertEquals(resultGuid, testGuid);
    }

    @Test(expected = WrongUuidFormatException.class)
    public void test_convert_null_to_UUID_throws_WrongUuidFormat() {
        String testGuidString = null;
        UUID resultGuid = StringToUuidConverter.convert(testGuidString);
    }

    @Test(expected = WrongUuidFormatException.class)
    public void test_convert_invalid_string_to_UUID_throws_WrongUuidFormat() {
        String testGuidString = "invalid-UUID";
        UUID resultGuid = StringToUuidConverter.convert(testGuidString);
    }

}