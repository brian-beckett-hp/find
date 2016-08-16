/*
 * Copyright 2015 Hewlett-Packard Development Company, L.P.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License.
 */

package com.hp.autonomy.frontend.find.hod.web;

enum SsoMvcConstants {
    AUTHENTICATE_PATH("authenticatePath"),
    COMBINED_REQUEST_API("combinedRequestApi"),
    ERROR_PAGE("errorPage"),
    LIST_APPLICATION_REQUEST("listApplicationRequest"),
    LOGOUT_ENDPOINT("endpoint"),
    LOGOUT_REDIRECT_URL("redirectUrl"),
    PATCH_REQUEST_API("combinedPatchRequestApi"),
    SSO_PAGE("ssoPage"),
    SSO_ENTRY_PAGE("ssoEntryPage");

    private final String value;

    SsoMvcConstants(final String value)
    {
        this.value = value;
    }

    public String value()
    {
        return value;
    }
}
