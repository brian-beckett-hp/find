/*
 * Copyright 2014-2015 Hewlett-Packard Development Company, L.P.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License.
 */

package com.hp.autonomy.frontend.find.configuration;

import com.hp.autonomy.frontend.configuration.ConfigService;
import com.hp.autonomy.frontend.configuration.HostAndPort;
import com.hp.autonomy.frontend.configuration.RedisConfig;
import com.hp.autonomy.hod.client.token.TokenRepository;
import com.hp.autonomy.hod.redis.RedisTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisSentinelPool;
import redis.clients.jedis.Protocol;
import redis.clients.util.Pool;

import java.util.HashSet;
import java.util.Set;

@Configuration
@Conditional(RedisCondition.class)
@EnableRedisHttpSession
public class RedisConfiguration {

    @Autowired
    private ConfigService<FindConfig> configService;

    @Bean
    public JedisConnectionFactory redisConnectionFactory() {
        final RedisConfig config = configService.getConfig().getRedis();
        final JedisConnectionFactory connectionFactory;

        //If we haven't specified any sentinels then assume non-distributed setup
        if (config.getSentinels().isEmpty()) {
            connectionFactory = new JedisConnectionFactory();
            connectionFactory.setHostName(config.getAddress().getHost());
            connectionFactory.setPort(config.getAddress().getPort());
        } else {
            final RedisSentinelConfiguration sentinelConfig = new RedisSentinelConfiguration().master(config.getMasterName());
            for (final HostAndPort node : config.getSentinels()) {
                sentinelConfig.sentinel(node.getHost(), node.getPort());
            }

            connectionFactory = new JedisConnectionFactory(sentinelConfig);
        }

        final Integer database = config.getDatabase();

        if (database != null) {
            connectionFactory.setDatabase(database);
        }

        connectionFactory.setPassword(config.getPassword());

        return connectionFactory;
    }

    @Bean
    public TokenRepository tokenRepository() {
        final RedisConfig redisConfig = configService.getConfig().getRedis();
        final JedisConnectionFactory jedisConnectionFactory = redisConnectionFactory();
        final Integer database = redisConfig.getDatabase();

        final Pool<Jedis> pool;

        if(redisConfig.getSentinels().isEmpty()) {
            final HostAndPort address = redisConfig.getAddress();

            if (database != null) {
                pool = new JedisPool(jedisConnectionFactory.getPoolConfig(), address.getHost(), address.getPort(), Protocol.DEFAULT_TIMEOUT, null, database);
            }
            else {
                pool = new JedisPool(jedisConnectionFactory.getPoolConfig(), address.getHost(), address.getPort());
            }
        }
        else {
            final Set<String> sentinels = new HashSet<>();

            for(final HostAndPort hostAndPort : redisConfig.getSentinels()) {
                sentinels.add(hostAndPort.getHost() + ':' + hostAndPort.getPort());
            }

            if (database != null) {
                pool = new JedisSentinelPool(redisConfig.getMasterName(), sentinels, jedisConnectionFactory.getPoolConfig(), Protocol.DEFAULT_TIMEOUT, null, database);
            }
            else {
                pool = new JedisSentinelPool(redisConfig.getMasterName(), sentinels, jedisConnectionFactory.getPoolConfig());
            }
        }

        return new RedisTokenRepository(pool);
    }

}