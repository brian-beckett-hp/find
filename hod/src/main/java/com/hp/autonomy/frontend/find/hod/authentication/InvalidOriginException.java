package com.hp.autonomy.frontend.find.hod.authentication;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.net.URL;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
class InvalidOriginException extends Exception {
    private static final long serialVersionUID = -4953273783001854237L;

    private final URL url;

    InvalidOriginException(final URL url) {
        super("Origin of url " + url + " not allowed");
        this.url = url;
    }
}
