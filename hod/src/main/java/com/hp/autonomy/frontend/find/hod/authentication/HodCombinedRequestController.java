/*
 * Copyright 2015 Hewlett-Packard Development Company, L.P.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License.
 */

package com.hp.autonomy.frontend.find.hod.authentication;

import com.hp.autonomy.frontend.configuration.ConfigService;
import com.hp.autonomy.frontend.find.core.web.ErrorResponse;
import com.hp.autonomy.frontend.find.hod.beanconfiguration.HodConfiguration;
import com.hp.autonomy.frontend.find.hod.configuration.HodFindConfig;
import com.hp.autonomy.hod.client.api.authentication.AuthenticationService;
import com.hp.autonomy.hod.client.api.authentication.AuthenticationToken;
import com.hp.autonomy.hod.client.api.authentication.EntityType;
import com.hp.autonomy.hod.client.api.authentication.SignedRequest;
import com.hp.autonomy.hod.client.api.authentication.TokenType;
import com.hp.autonomy.hod.client.error.HodErrorException;
import com.hp.autonomy.hod.sso.HodAuthenticationRequestService;
import com.hp.autonomy.hod.sso.UnboundTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;

@RestController
public class HodCombinedRequestController {

    public static final String COMBINED_REQUEST = "/api/authentication/combined-request";
    public static final String COMBINED_PATCH_REQUEST = "/api/authentication/combined-patch-request";

    private final HodAuthenticationRequestService tokenService;
    private final ConfigService<HodFindConfig> configService;
    private final UnboundTokenService<TokenType.HmacSha1> unboundTokenService;
    private final String ssoPageOrigin;
    private final AuthenticationService authenticationService;

    @Autowired
    public HodCombinedRequestController(
            final HodAuthenticationRequestService tokenService,
            final ConfigService<HodFindConfig> configService,
            final UnboundTokenService<TokenType.HmacSha1> unboundTokenService,
            final AuthenticationService authenticationService,
            @Value(HodConfiguration.SSO_PAGE_PROPERTY) final String ssoPageUrl
    ) {
        this.tokenService = tokenService;
        this.configService = configService;
        this.unboundTokenService = unboundTokenService;
        this.authenticationService = authenticationService;

        try {
            ssoPageOrigin = resolveOrigin(new URL(ssoPageUrl));
        } catch (final MalformedURLException e) {
            throw new IllegalArgumentException("Malformed SSO page url " + ssoPageUrl, e);
        }
    }

    @RequestMapping(value = COMBINED_REQUEST, method = RequestMethod.GET)
    public SignedRequest getCombinedRequest(
            @RequestParam("domain") final String domain,
            @RequestParam("application") final String application,
            @RequestParam("user-store-domain") final String userStoreDomain,
            @RequestParam("user-store-name") final String userStoreName
    ) throws HodErrorException {
        return tokenService.getCombinedRequest(domain, application, userStoreDomain, userStoreName);
    }

    @RequestMapping(value = COMBINED_PATCH_REQUEST, method = RequestMethod.GET)
    public SignedRequest getCombinedPatchRequest(
            @RequestParam("redirect-url") final URL redirectUrl
    ) throws HodErrorException, InvalidOriginException {
        final String redirectOrigin = resolveOrigin(redirectUrl);

        // Ensure that the redirect URL is from an allowed origin; otherwise, a malicious website could get access to the
        // user's combined SSO token
        if (configService.getConfig().getAllowedOrigins().contains(redirectOrigin)) {
            final AuthenticationToken<EntityType.Unbound, TokenType.HmacSha1> token = unboundTokenService.getUnboundToken();
            return authenticationService.combinedPatchRequest(Collections.singletonList(ssoPageOrigin), redirectUrl.toString(), token);
        } else {
            throw new InvalidOriginException(redirectUrl);
        }
    }

    @ExceptionHandler(InvalidOriginException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInvalidOrigin(final InvalidOriginException exception) {
        return new ErrorResponse("Origin of redirect URL " + exception.getUrl() + " not allowed");
    }

    private String resolveOrigin(final URL url) {
        return url.getProtocol() + "://" + url.getAuthority();
    }
}
