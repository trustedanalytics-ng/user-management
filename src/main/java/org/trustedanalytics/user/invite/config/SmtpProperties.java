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
package org.trustedanalytics.user.invite.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Objects;

@Component

@Getter @Setter
@ConfigurationProperties("smtp")
public class SmtpProperties {

    private String protocol;
    private String host;
    private String username;
    private String password;
    private String email;
    private int port;
    private int timeout;
    private boolean debug;
    private String emailName;

    public int hashCode() {
        return Objects.hash(timeout, debug, emailName, host, protocol, username, password, port);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        
        SmtpProperties other;
        if (obj instanceof SmtpProperties) {
            other = (SmtpProperties) obj;
            return allTrue(
                   Objects.equals(timeout, other.timeout),
                   Objects.equals(debug, other.debug),
                   Objects.equals(host, other.host),
                   Objects.equals(emailName, other.emailName),
                   Objects.equals(protocol, other.protocol),
                   Objects.equals(username, other.username),
                   Objects.equals(password, other.password),
                   Objects.equals(port, other.port));
        }
        
        return false;
    }

    private boolean allTrue(Boolean... args) {
        return !Arrays.asList(args).contains(false);
    }
}
