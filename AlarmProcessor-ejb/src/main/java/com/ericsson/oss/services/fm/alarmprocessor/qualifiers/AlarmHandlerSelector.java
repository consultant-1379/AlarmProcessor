/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.fm.alarmprocessor.qualifiers;

import javax.enterprise.util.AnnotationLiteral;

/**
 * Class for AlarmHandler Selector based on alarm Record Type.
 */
public final class AlarmHandlerSelector extends AnnotationLiteral<AlarmHandlerQualifier> implements AlarmHandlerQualifier {

    private static final long serialVersionUID = 512520115980564253L;

    private final RecordTypes recordType;

    private AlarmHandlerSelector(final RecordTypes recordType) {
        this.recordType = recordType;
    }

    @Override
    public RecordTypes value() {
        return recordType;
    }

    public static AlarmHandlerSelector recordType(final RecordTypes value) {
        return new AlarmHandlerSelector(value);
    }
}
