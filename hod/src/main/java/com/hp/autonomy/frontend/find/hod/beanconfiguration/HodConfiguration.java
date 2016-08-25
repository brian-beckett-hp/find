/*
 * Copyright 2015 Hewlett-Packard Development Company, L.P.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License.
 */

package com.hp.autonomy.frontend.find.hod.beanconfiguration;

import com.autonomy.aci.client.services.AciService;
import com.autonomy.aci.client.services.impl.AciServiceImpl;
import com.autonomy.aci.client.transport.impl.AciHttpClientImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.autonomy.frontend.configuration.Authentication;
import com.hp.autonomy.frontend.configuration.AuthenticationConfig;
import com.hp.autonomy.frontend.configuration.ConfigService;
import com.hp.autonomy.frontend.configuration.SingleUserAuthenticationValidator;
import com.hp.autonomy.frontend.find.hod.configuration.HodAuthenticationMixins;
import com.hp.autonomy.frontend.find.hod.configuration.HodFindConfig;
import com.hp.autonomy.hod.client.api.authentication.AuthenticationService;
import com.hp.autonomy.hod.client.api.authentication.AuthenticationServiceImpl;
import com.hp.autonomy.hod.client.api.authentication.EntityType;
import com.hp.autonomy.hod.client.api.authentication.TokenType;
import com.hp.autonomy.hod.client.api.userstore.user.UserStoreUsersService;
import com.hp.autonomy.hod.client.api.userstore.user.UserStoreUsersServiceImpl;
import com.hp.autonomy.hod.client.config.HodServiceConfig;
import com.hp.autonomy.hod.client.error.HodErrorException;
import com.hp.autonomy.hod.client.token.TokenProxyService;
import com.hp.autonomy.hod.client.token.TokenRepository;
import com.hp.autonomy.hod.sso.*;
import com.hp.autonomy.idolutils.processors.AciResponseJaxbProcessorFactory;
import com.hp.autonomy.user.UserService;
import com.hp.autonomy.user.UserServiceImpl;
import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

@Configuration
public class HodConfiguration {
    public static final String SSO_GET_PAGE_PROPERTY = "${find.hod.sso:https://www.havenondemand.com/sso.html}";
    public static final String SSO_POST_PAGE_PROPERTY = "${find.hod.ssoPost:https://www.havenondemand.com/sso}";
    public static final String HOD_API_URL_PROPERTY = "${find.iod.api:https://api.havenondemand.com}";
    private static final int HTTP_SOCKET_TIMEOUT = 90000;
    private static final int HTTP_MAX_CONNECTIONS_PER_ROUTE = 20;
    private static final int HTTP_MAX_CONNECTIONS_TOTAL = 120;

    @Autowired
    private Environment environment;

    @Autowired
    private TokenRepository tokenRepository;

    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Bean
    @Primary
    @Autowired
    public ObjectMapper jacksonObjectMapper(final Jackson2ObjectMapperBuilder builder) {
        return builder.createXmlMapper(false)
                .mixIn(Authentication.class, HodAuthenticationMixins.class)
                .build();
    }

    @Bean
    public SingleUserAuthenticationValidator singleUserAuthenticationValidator(final ConfigService<? extends AuthenticationConfig<?>> configService) {
        final SingleUserAuthenticationValidator singleUserAuthenticationValidator = new SingleUserAuthenticationValidator();
        singleUserAuthenticationValidator.setConfigService(configService);

        return singleUserAuthenticationValidator;
    }

    @Bean
    public HttpClient httpClient() {
        final HttpClientBuilder builder = HttpClientBuilder.create();

        final String proxyHost = environment.getProperty("find.https.proxyHost");

        if (proxyHost != null) {
            final Integer proxyPort = Integer.valueOf(environment.getProperty("find.https.proxyPort", "8080"));
            builder.setProxy(new HttpHost(proxyHost, proxyPort));
        }

        builder.disableCookieManagement();

        return builder.build();
    }

    @Bean
    public HodServiceConfig.Builder<EntityType.Combined, TokenType.Simple> hodServiceConfigBuilder(final HttpClient httpClient, @Qualifier("hodSearchResultObjectMapper") final ObjectMapper hodSearchResultObjectMapper) {
        final String endpoint = environment.getProperty("find.iod.api", "https://api.havenondemand.com");

        return new HodServiceConfig.Builder<EntityType.Combined, TokenType.Simple>(endpoint)
                .setHttpClient(httpClient)
                .setObjectMapper(hodSearchResultObjectMapper)
                .setTokenRepository(tokenRepository);
    }

    @Bean
    public HodServiceConfig<EntityType.Combined, TokenType.Simple> hodServiceConfig(
            final HodServiceConfig.Builder<EntityType.Combined, TokenType.Simple> hodServiceConfigBuilder,
            final TokenProxyService<EntityType.Combined, TokenType.Simple> tokenProxyService) {
        return hodServiceConfigBuilder
                .setTokenProxyService(tokenProxyService)
                .build();
    }

    @Bean
    public AuthenticationService authenticationService(final HodServiceConfig.Builder<EntityType.Combined, TokenType.Simple> hodServiceConfigBuilder) {
        return new AuthenticationServiceImpl(hodServiceConfigBuilder.build());
    }

    @Bean
    public HodAuthenticationRequestService hodAuthenticationRequestService(final ConfigService<? extends HodSsoConfig> configService, final AuthenticationService authenticationService, final UnboundTokenService<TokenType.HmacSha1> unboundTokenService) {
        return new HodAuthenticationRequestServiceImpl(configService, authenticationService, unboundTokenService);
    }

    @Bean
    public UnboundTokenService<TokenType.HmacSha1> unboundTokenService(final ConfigService<? extends HodSsoConfig> configService, final AuthenticationService authenticationService) throws HodErrorException {
        return new UnboundTokenServiceImpl(authenticationService, configService);
    }

    @Bean
    public UserStoreUsersService userStoreUsersService(final HodServiceConfig<EntityType.Combined, TokenType.Simple> hodServiceConfig) {
        return new UserStoreUsersServiceImpl(hodServiceConfig);
    }

    @Bean
    public UserService userService(final ConfigService<HodFindConfig> configService, final AciService aciService, final AciResponseJaxbProcessorFactory processorFactory) {
        return new UserServiceImpl(configService, aciService, processorFactory);
    }

    @Bean
    public AciResponseJaxbProcessorFactory processorFactory() {
        return new AciResponseJaxbProcessorFactory();
    }

    @Bean
    public AciService aciService() {
        return new AciServiceImpl(new AciHttpClientImpl(aciHttpClient()));
    }

    @Bean
    public HttpClient aciHttpClient() {
        return createHttpClient(HTTP_SOCKET_TIMEOUT, HTTP_MAX_CONNECTIONS_PER_ROUTE, HTTP_MAX_CONNECTIONS_TOTAL);
    }

    private HttpClient createHttpClient(final int httpSocketTimeout, final int maxConnectionsPerRoute, final int maxConnectionsTotal) {
        final SocketConfig socketConfig = SocketConfig.custom()
                .setSoTimeout(httpSocketTimeout)
                .build();

        return HttpClientBuilder.create()
                .setMaxConnPerRoute(maxConnectionsPerRoute)
                .setMaxConnTotal(maxConnectionsTotal)
                .setDefaultSocketConfig(socketConfig)
                .build();
    }
}
