/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.fm.alarmprocessor.integration.test.util;

import java.io.UnsupportedEncodingException;

import javax.xml.bind.DatatypeConverter;

import org.apache.http.client.methods.HttpRequestBase;

public class AuthenticationHandler {

    private static final String USERNAME = "pibUser";
    private static final String PASSWORD = "3ric550N*";

    public static HttpRequestBase addUserPassword(final HttpRequestBase request) {
        try {
            final String encoding = DatatypeConverter
                    .printBase64Binary((USERNAME + ":" + PASSWORD)
                            .getBytes("UTF-8"));
            request.setHeader("Authorization", "Basic " + encoding);
        } catch (final UnsupportedEncodingException canBeIgnored) {
        }
        return request;
    }
}
