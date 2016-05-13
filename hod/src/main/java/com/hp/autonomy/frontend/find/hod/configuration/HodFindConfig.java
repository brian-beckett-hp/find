/*
 * Copyright 2015 Hewlett-Packard Development Company, L.P.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License.
 */

package com.hp.autonomy.frontend.find.hod.configuration;

import com.autonomy.aci.client.transport.AciServerDetails;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.hp.autonomy.frontend.configuration.*;
import com.hp.autonomy.frontend.find.core.configuration.FindConfig;
import com.hp.autonomy.frontend.find.core.configuration.MapConfiguration;
import com.hp.autonomy.frontend.find.core.configuration.SavedSearchConfig;
import com.hp.autonomy.hod.client.api.authentication.ApiKey;
import com.hp.autonomy.hod.sso.HodSsoConfig;
import com.hp.autonomy.searchcomponents.core.config.FieldsInfo;
import com.hp.autonomy.searchcomponents.hod.configuration.HodSearchCapable;
import com.hp.autonomy.searchcomponents.hod.configuration.QueryManipulationConfig;
import com.hp.autonomy.user.UserServiceConfig;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jasypt.util.text.TextEncryptor;

import java.util.List;
import java.util.Set;

@SuppressWarnings({"InstanceVariableOfConcreteClass", "DefaultAnnotationParam"})
@JsonDeserialize(builder = HodFindConfig.Builder.class)
@Getter
@EqualsAndHashCode(callSuper = false)
public class HodFindConfig extends AbstractConfig<HodFindConfig> implements AuthenticationConfig<HodFindConfig>, HodSearchCapable, PasswordsConfig<HodFindConfig>, HodSsoConfig, FindConfig, UserServiceConfig {
    private final Authentication<?> login;
    private final HsodConfig hsod;
    private final IodConfig iod;
    private final QueryManipulationConfig queryManipulation;
    private final Set<String> allowedOrigins;
    private final RedisConfig redis;
    private final FieldsInfo fieldsInfo;
    private final MapConfiguration map;
    private final Integer minScore;
    private final Set<String> fieldBlacklist;
    private final List<HodParametricDisplayValues> parametricDisplayValues;
    private final ServerConfig securityInfoCommunity;

    @JsonProperty("savedSearches")
    private final SavedSearchConfig savedSearchConfig;

    private HodFindConfig(final Builder builder) {
        login = builder.login;
        hsod = builder.hsod;
        iod = builder.iod;
        allowedOrigins = builder.allowedOrigins;
        redis = builder.redis;
        fieldsInfo = builder.fieldsInfo;
        queryManipulation = builder.queryManipulation;
        map = builder.map;
        savedSearchConfig = builder.savedSearchConfig;
        minScore = builder.minScore;
        parametricDisplayValues = builder.parametricDisplayValues;
        fieldBlacklist = builder.fieldBlacklist;
        securityInfoCommunity = builder.securityInfoCommunity;
    }

    @SuppressWarnings("OverlyComplexMethod")
    @Override
    public HodFindConfig merge(final HodFindConfig config) {
        return config != null ? new Builder()
                .setLogin(login == null ? config.login : login.merge(config.login))
                .setIod(iod == null ? config.iod : iod.merge(config.iod))
                .setAllowedOrigins(allowedOrigins == null ? config.allowedOrigins : allowedOrigins)
                .setRedis(redis == null ? config.redis : redis.merge(config.redis))
                .setQueryManipulation(queryManipulation == null ? config.queryManipulation : queryManipulation.merge(config.queryManipulation))
                .setHsod(hsod == null ? config.hsod : hsod.merge(config.hsod))
                .setFieldsInfo(fieldsInfo == null ? config.fieldsInfo : fieldsInfo.merge(config.fieldsInfo))
                .setMap(map == null ? config.map : map.merge(config.map))
                .setSavedSearchConfig(savedSearchConfig == null ? config.savedSearchConfig : savedSearchConfig.merge(config.savedSearchConfig))
                .setSecurityInfoCommunity(securityInfoCommunity == null ? config.securityInfoCommunity : securityInfoCommunity.merge(config.securityInfoCommunity))
                .setMinScore(minScore == null ? config.minScore : minScore)
                .setParametricDisplayValues(parametricDisplayValues == null ? config.parametricDisplayValues : parametricDisplayValues)
                .setFieldBlacklist(fieldBlacklist == null ? config.fieldBlacklist : fieldBlacklist)
                .build() : this;
    }

    @JsonIgnore
    @Override
    public AciServerDetails getCommunityDetails() {
        return securityInfoCommunity.toAciServerDetails();
    }

    @Override
    public HodFindConfig withoutDefaultLogin() {
        final Builder builder = new Builder(this);

        builder.login = builder.login.withoutDefaultLogin();

        return builder.build();
    }

    @Override
    public HodFindConfig generateDefaultLogin() {
        final Builder builder = new Builder(this);

        builder.login = builder.login.generateDefaultLogin();

        return builder.build();
    }

    @Override
    public HodFindConfig withHashedPasswords() {
        final Builder builder = new Builder(this);

        builder.login = builder.login.withHashedPasswords();

        return builder.build();
    }

    @Override
    public void basicValidate() throws ConfigException {
        redis.basicValidate();
        queryManipulation.basicValidate();
        savedSearchConfig.basicValidate();

        if (map != null) {
            map.basicValidate("map");
        }

        if (!"default".equalsIgnoreCase(login.getMethod())) {
            login.basicValidate();
        }
    }

    @Override
    public HodFindConfig withoutPasswords() {
        final Builder builder = new Builder(this);

        builder.login = login.withoutPasswords();

        return builder.build();
    }

    @Override
    public HodFindConfig withEncryptedPasswords(final TextEncryptor encryptor) {
        return this;
    }

    @Override
    public HodFindConfig withDecryptedPasswords(final TextEncryptor encryptor) {
        return this;
    }

    @Override
    @JsonIgnore
    public Authentication<?> getAuthentication() {
        return login;
    }

    @Override
    @JsonIgnore
    public ApiKey getApiKey() {
        return iod.getApiKey();
    }

    @JsonPOJOBuilder(withPrefix = "set")
    @NoArgsConstructor
    @Setter
    @Accessors(chain = true)
    public static class Builder {
        private Authentication<?> login;
        private HsodConfig hsod;
        private IodConfig iod;
        private Set<String> allowedOrigins;
        private RedisConfig redis;
        private QueryManipulationConfig queryManipulation;
        private FieldsInfo fieldsInfo;
        private MapConfiguration map;
        private Integer minScore;
        private Set<String> fieldBlacklist;
        private List<HodParametricDisplayValues> parametricDisplayValues;
        @JsonProperty("savedSearches")
        private SavedSearchConfig savedSearchConfig;
        private ServerConfig securityInfoCommunity;

        public Builder(final HodFindConfig config) {
            login = config.login;
            hsod = config.hsod;
            iod = config.iod;
            allowedOrigins = config.allowedOrigins;
            redis = config.redis;
            queryManipulation = config.queryManipulation;
            fieldsInfo = config.fieldsInfo;
            map = config.map;
            minScore = config.minScore;
            fieldBlacklist = config.fieldBlacklist;
            parametricDisplayValues = config.parametricDisplayValues;
            savedSearchConfig = config.savedSearchConfig;
            securityInfoCommunity = config.securityInfoCommunity;
        }

        public HodFindConfig build() {
            return new HodFindConfig(this);
        }
    }
}
