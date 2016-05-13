/*
 * Copyright 2015 Hewlett-Packard Development Company, L.P.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License.
 */

package com.hp.autonomy.frontend.find.hod.beanconfiguration;

import com.hp.autonomy.frontend.find.core.beanconfiguration.DispatcherServletConfiguration;
import com.hp.autonomy.frontend.find.core.beanconfiguration.FindRole;
import com.hp.autonomy.frontend.find.core.web.FindController;
import com.hp.autonomy.frontend.find.hod.authentication.FindSecurityInfoRetrieverService;
import com.hp.autonomy.frontend.find.hod.authentication.HavenSearchUserMetadata;
import com.hp.autonomy.frontend.find.hod.authentication.HsodUsernameResolver;
import com.hp.autonomy.frontend.find.hod.web.SsoController;
import com.hp.autonomy.hod.client.api.authentication.AuthenticationService;
import com.hp.autonomy.hod.client.api.authentication.TokenType;
import com.hp.autonomy.hod.client.api.userstore.user.UserStoreUsersService;
import com.hp.autonomy.hod.client.token.TokenRepository;
import com.hp.autonomy.hod.sso.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

@Configuration
@Order(99)
public class HodSecurity extends WebSecurityConfigurerAdapter {

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private UnboundTokenService<TokenType.HmacSha1> unboundTokenService;

    @Autowired
    private UserStoreUsersService userStoreUsersService;

    @SuppressWarnings("ProhibitedExceptionDeclared")
    @Override
    protected void configure(final AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(new HodAuthenticationProvider(
                tokenRepository,
                FindRole.USER.toString(),
                authenticationService,
                unboundTokenService,
                userStoreUsersService,
                HavenSearchUserMetadata.METADATA_TYPES,
                usernameResolver(),
                securityInfoRetrieverService()
        ));
    }

    @SuppressWarnings("ProhibitedExceptionDeclared")
    @Override
    protected void configure(final HttpSecurity http) throws Exception {
        final AuthenticationEntryPoint ssoEntryPoint = new SsoAuthenticationEntryPoint(SsoController.SSO_PAGE);

        final SsoAuthenticationFilter ssoAuthenticationFilter = new SsoAuthenticationFilter(SsoController.SSO_AUTHENTICATION_URI);
        ssoAuthenticationFilter.setAuthenticationManager(authenticationManager());

        final LogoutSuccessHandler logoutSuccessHandler = new HodTokenLogoutSuccessHandler(SsoController.SSO_LOGOUT_PAGE, tokenRepository);

        http.regexMatcher("/public/.*|/sso|/authenticate-sso|/api/authentication/.*|/logout")
                .csrf()
                    .disable()
                .exceptionHandling()
                    .authenticationEntryPoint(ssoEntryPoint)
                    .accessDeniedPage(DispatcherServletConfiguration.AUTHENTICATION_ERROR_PATH)
                    .and()
                .authorizeRequests()
                    .antMatchers(FindController.APP_PATH + "**").hasRole(FindRole.USER.name())
                    .and()
                .logout()
                    .logoutSuccessHandler(logoutSuccessHandler)
                    .and()
                .addFilterAfter(ssoAuthenticationFilter, AbstractPreAuthenticatedProcessingFilter.class);
    }

    @Bean
    public HsodUsernameResolver usernameResolver() {
        return new HsodUsernameResolver();
    }

    @Bean
    public SecurityInfoRetriever securityInfoRetrieverService() {
        return new FindSecurityInfoRetrieverService();
    }

}
