/**
 *  Copyright (c) 2016 Intel Corporation 
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
package org.trustedanalytics.user;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy.LowerCaseWithUnderscoresStrategy;
import feign.Feign;
import feign.Logger;
import feign.Request;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.slf4j.Slf4jLogger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.trustedanalytics.user.common.OAuth2PrivilegedInterceptor;
import org.trustedanalytics.user.manageusers.AuthGatewayOperations;

import java.util.concurrent.TimeUnit;

@Configuration
public class ApplicationConfiguration {

    @Value("${authgateway.host}")
    private String authGatewayUrl;

    /**
     * Calls to auth-gateway needs to be performed using privileged interceptor because users
     * that try to register for the first time don't have jwt token yet.
     */
    @Bean
    public AuthGatewayOperations authgatewayOperations(OAuth2PrivilegedInterceptor interceptor) {
        final int connectTimeout = (int) TimeUnit.SECONDS.toMillis(30);
        final int readTimeout = (int) TimeUnit.MINUTES.toMillis(60);
        final ObjectMapper objectMapper = new ObjectMapper()
            .setPropertyNamingStrategy(new LowerCaseWithUnderscoresStrategy())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        return Feign.builder()
            .encoder(new JacksonEncoder(objectMapper))
            .decoder(new JacksonDecoder(objectMapper))
            .requestInterceptor(interceptor)
            .options(new Request.Options(connectTimeout, readTimeout))
            .logger(new Slf4jLogger(AuthGatewayOperations.class))
            .logLevel(Logger.Level.BASIC)
            .target(AuthGatewayOperations.class, authGatewayUrl);
    }
}
