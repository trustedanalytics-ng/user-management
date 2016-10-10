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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class UaaProblem {

    private String message;
    private String error;

    @JsonProperty("user_id")
    private String userId;

    public UaaProblem() {
    }

    public UaaProblem(String message, String error, String userId) {
        this.message = message;
        this.error = error;
        this.userId = userId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(message, error, userId);
    }

    @Override public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        UaaProblem other = (UaaProblem) obj;
        return Objects.equals(this.message, other.message) && Objects
            .equals(this.error, other.error) && Objects.equals(this.userId, other.userId);
    }

}
