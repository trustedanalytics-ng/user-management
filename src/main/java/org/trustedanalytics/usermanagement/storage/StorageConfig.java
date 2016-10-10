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
package org.trustedanalytics.usermanagement.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.trustedanalytics.redis.encryption.EncryptionService;
import org.trustedanalytics.redis.encryption.HashService;
import org.trustedanalytics.redis.encryption.serializer.HashedStringRedisSerializer;
import org.trustedanalytics.redis.encryption.serializer.SecureJacksonJsonRedisSerializer;
import org.trustedanalytics.usermanagement.invitations.service.AccessInvitations;
import org.trustedanalytics.usermanagement.invitations.securitycode.SecurityCode;
import org.trustedanalytics.usermanagement.invitations.securitycode.SecurityCodeService;
import org.trustedanalytics.usermanagement.invitations.service.AccessInvitationsService;

public class StorageConfig {

    private StorageConfig() {
    }

    @Configuration
    public class RedisConnectionFactoryConfig {

        @Value("${redis.host}")
        private String redisHost;

        @Value("${redis.port}")
        private int redisPort;

        @Bean
        RedisConnectionFactory redisConnectionFactory() {
            JedisConnectionFactory factory = new JedisConnectionFactory();
            factory.setHostName(redisHost);
            factory.setPort(redisPort);
            factory.setUsePool(true);
            return factory;
        }
    }

    @Profile("in-memory")
    @Configuration
    public static class InMemorySecurityCodesStorageConfig {

        @Bean
        KeyValueStore<SecurityCode> inMemorySecurityCodeStore() {
            return new InMemoryStore<>();
        }

        @Bean
        SecurityCodeService inMemorySecurityCodeService(KeyValueStore<SecurityCode> inMemorySecurityCodeStore) {
            return new SecurityCodeService(inMemorySecurityCodeStore);
        }
    }

    @Profile("in-memory")
    @Configuration
    public static class InMemoryInvitationsStorageConfig {
        @Bean
        KeyValueStore<AccessInvitations> inMemoryAccessInvitationsStore() {
            return new InMemoryStore<>();
        }

        @Bean
        AccessInvitationsService inMemoryAccessInvitationsService(KeyValueStore<AccessInvitations> inMemoryAccessInvitationsStore) {
            return new AccessInvitationsService(inMemoryAccessInvitationsStore);
        }
    }



    @Profile("redis")
    @Configuration
    public static class RedisSecurityConfig {

        @Value("${security.codes.db.cipher.key}")
        private String cipher;

        @Value("${security.codes.db.hash.salt}")
        private String salt;

        @Bean
        protected EncryptionService encryptionService() {
            return new EncryptionService(cipher);
        }

        @Bean
        protected HashService hashService() {
            return new HashService(salt);
        }

        @Bean
        protected HashedStringRedisSerializer secureStringRedisSerializer(HashService hashService) {
            return new HashedStringRedisSerializer(hashService);
        }
    }

    @Profile("redis")
    @Configuration
    public static class RedisSecurityCodesStorageConfig {

        @Bean
        KeyValueStore<SecurityCode> redisSecurityCodeStore( RedisOperations<String, SecurityCode> redisTemplate) {
            return new RedisStore<>(redisTemplate, "security-codes");
        }

        @Bean
        protected SecurityCodeService redisSecurityCodeService(KeyValueStore<SecurityCode> redisSecurityCodeStore) {
            return new SecurityCodeService(redisSecurityCodeStore);
        }

        @Bean
        SecureJacksonJsonRedisSerializer<SecurityCode> secureJacksonJsonRedisSerializer(EncryptionService encryptionService) {
            return new SecureJacksonJsonRedisSerializer<SecurityCode>(SecurityCode.class, encryptionService);
        }

        @Bean
        public RedisOperations<String, SecurityCode> redisTemplate(RedisConnectionFactory redisConnectionFactory,
                                                                   HashedStringRedisSerializer hashedStringRedisSerializer,
                                                                   SecureJacksonJsonRedisSerializer<SecurityCode> secureJacksonJsonRedisSerializer) {
            return CommonConfiguration.redisTemplate(redisConnectionFactory,
                    hashedStringRedisSerializer,
                    secureJacksonJsonRedisSerializer);
        }
    }

    @Profile("redis")
    @Configuration
    public static class RedisInvitationStorageConfig {
        @Bean
        public KeyValueStore<AccessInvitations> redisAccessInvitationsStore(
                RedisOperations<String, AccessInvitations> redisAccessInvitationsTemplate) {
            return new RedisStore<>(redisAccessInvitationsTemplate, "access-invitations");
        }

        @Bean
        AccessInvitationsService redisAccessInvitationsService(KeyValueStore<AccessInvitations> redisAccessInvitationsStore) {
            return new AccessInvitationsService(redisAccessInvitationsStore);
        }

        @Bean
        public RedisOperations<String, AccessInvitations> redisAccessInvitationsTemplate(RedisConnectionFactory redisConnectionFactory,
                                                                                         HashedStringRedisSerializer hashedStringRedisSerializer) {
            return CommonConfiguration.redisTemplate(redisConnectionFactory,
                    hashedStringRedisSerializer,
                    new JacksonJsonRedisSerializer<AccessInvitations>(AccessInvitations.class));
        }
    }

    private static class CommonConfiguration {
        private CommonConfiguration() {
        }

        private static <T> RedisOperations<String, T> redisTemplate(RedisConnectionFactory redisConnectionFactory,
                                                                    RedisSerializer<String> keySerializer,
                                                                    RedisSerializer<T> valueSerializer) {
            RedisTemplate<String, T> template = new RedisTemplate<String, T>();
            template.setConnectionFactory(redisConnectionFactory);

            RedisSerializer<String> stringSerializer = new StringRedisSerializer();
            template.setKeySerializer(stringSerializer);
            template.setValueSerializer(valueSerializer);
            template.setHashKeySerializer(keySerializer);
            template.setHashValueSerializer(valueSerializer);

            return template;
        }
    }
}
