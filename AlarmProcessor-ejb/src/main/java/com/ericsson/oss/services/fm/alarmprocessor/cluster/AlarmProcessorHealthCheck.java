/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2022
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.fm.alarmprocessor.cluster;

import static javax.ejb.ConcurrencyManagementType.BEAN;
import static javax.ejb.TransactionAttributeType.NOT_SUPPORTED;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;

@Singleton
@TransactionAttribute(NOT_SUPPORTED)
@ConcurrencyManagement(BEAN)
public class AlarmProcessorHealthCheck {

    private boolean syncState;

    public boolean checkHealthState() {
        return syncState;
    }

    public void setHealthState(final boolean newState) {
        this.syncState = newState;
    }
}
