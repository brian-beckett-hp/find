/*
 * Copyright 2015 Hewlett-Packard Development Company, L.P.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License.
 */

package com.hp.autonomy.frontend.find.core.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hp.autonomy.frontend.configuration.AuthenticationConfig;
import com.hp.autonomy.frontend.configuration.ConfigService;
import com.hp.autonomy.frontend.configuration.LoginTypes;
import com.hp.autonomy.frontend.find.core.beanconfiguration.AppConfiguration;
import com.hp.autonomy.frontend.find.core.configuration.FindConfig;
import com.hp.autonomy.searchcomponents.core.authentication.AuthenticationInformationRetriever;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public abstract class FindController {
    public static final String APP_PATH = "/public/";
    public static final String LOGIN_PATH = "/login";
    public static final String DEFAULT_LOGIN_PAGE = "/loginPage";
    public static final String CONFIG_PATH = "/config";

    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Autowired
    private ConfigService<? extends AuthenticationConfig<?>> authenticationConfigService;

    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Autowired
    private ConfigService<? extends FindConfig> configService;

    @Value(AppConfiguration.GIT_COMMIT_PROPERTY)
    private String gitCommit;

    @Value(AppConfiguration.APPLICATION_RELEASE_VERSION_PROPERTY)
    private String releaseVersion;

    @Autowired
    private ControllerUtils controllerUtils;

    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Autowired
    private AuthenticationInformationRetriever<?, ? extends Principal> authenticationInformationRetriever;

    protected abstract Map<String, Object> getPublicConfig();

    @RequestMapping("/")
    public void index(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        final String contextPath = request.getContextPath();

        if (LoginTypes.DEFAULT.equals(authenticationConfigService.getConfig().getAuthentication().getMethod())) {
            response.sendRedirect(contextPath + DEFAULT_LOGIN_PAGE);
        } else {
            response.sendRedirect(contextPath + APP_PATH);
        }
    }

    @RequestMapping(value = APP_PATH, method = RequestMethod.GET)
    public ModelAndView mainPage() throws JsonProcessingException {
        final String username = authenticationInformationRetriever.getAuthentication().getName();

        final Collection<String> roles = new LinkedList<>();

        for (final GrantedAuthority authority : authenticationInformationRetriever.getAuthentication().getAuthorities()) {
            roles.add(authority.getAuthority());
        }

        final Map<String, Object> config = new HashMap<>();
        config.put(MvcConstants.USERNAME.value(), username);
        config.put(MvcConstants.ROLES.value(), roles);
        config.put(MvcConstants.GIT_COMMIT.value(), gitCommit);
        config.put(MvcConstants.RELEASE_VERSION.value(), releaseVersion);
        config.put(MvcConstants.MAP.value(), configService.getConfig().getMap());
        config.put(MvcConstants.SAVED_SEARCH_CONFIG.value(), configService.getConfig().getSavedSearchConfig());
        config.put(MvcConstants.MIN_SCORE.value(), configService.getConfig().getMinScore());
        config.putAll(getPublicConfig());

        final Map<String, Object> attributes = new HashMap<>();
        attributes.put(MvcConstants.GIT_COMMIT.value(), gitCommit);
        attributes.put(MvcConstants.CONFIG.value(), controllerUtils.convertToJson(config));

        return new ModelAndView(ViewNames.APP.viewName(), attributes);
    }

    @RequestMapping(value = LOGIN_PATH, method = RequestMethod.GET)
    public ModelAndView login() {
        final Map<String, Object> attributes = new HashMap<>();
        attributes.put(MvcConstants.GIT_COMMIT.value(), gitCommit);
        return new ModelAndView(ViewNames.LOGIN.viewName(), attributes);
    }

    @RequestMapping(value = CONFIG_PATH, method = RequestMethod.GET)
    public ModelAndView config() {
        final Map<String, Object> attributes = new HashMap<>();
        attributes.put(MvcConstants.GIT_COMMIT.value(), gitCommit);
        return new ModelAndView(ViewNames.CONFIG.viewName(), attributes);
    }
}
