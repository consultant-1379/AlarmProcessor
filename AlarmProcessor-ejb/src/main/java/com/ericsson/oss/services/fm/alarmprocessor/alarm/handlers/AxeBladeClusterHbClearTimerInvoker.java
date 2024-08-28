/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.fm.alarmprocessor.alarm.handlers;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;

/**
 * Class responsible for invoking AxeBladeClusterHbClearTimer.
 */
@ApplicationScoped
public class AxeBladeClusterHbClearTimerInvoker {

    private static final Logger LOGGER = LoggerFactory.getLogger(AxeBladeClusterHbClearTimerInvoker.class);

    @Inject
    private AxeBladeClusterHbClearTimer axeBladeClusterHbClearTimer;

    public void changeCurrentServiceStateByTimer(final ProcessedAlarmEvent alarmRecord) {
        LOGGER.debug("invoking AxeBladeClusterHbClearTimer for {}", alarmRecord.getObjectOfReference());
        axeBladeClusterHbClearTimer.changeCurrentServiceStateByTimer(alarmRecord);
    }
}
