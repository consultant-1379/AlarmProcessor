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

package com.ericsson.oss.services.fm.alarmprocessor.orphanclear;

import com.ericsson.oss.services.fm.alarmprocessor.eventhandlers.AlarmHandler;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmCorrelator;
import com.ericsson.oss.services.fm.alarmprocessor.util.ClearAlarmsCacheManager;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

/**
 * Correlated Expired Clear Alarm Handler class.
 */
@Stateless
public class CorrelatedExpiredClearAlarmHandler {

    @Inject
    AlarmHandler alarmHandler;

    @Inject
    private ClearAlarmsCacheManager clearAlarmsCacheManager;

    @Inject
    private AlarmCorrelator alarmCorrelator;

    /**
     * onEvent method callback.
     * @param alarmRecord the ProcessedAlarmEvent.
     */
    public void onEvent(final ProcessedAlarmEvent alarmRecord) {
        this.alarmHandler.onEvent(alarmRecord);
    }

    /**
     * removeClearAlarm method callback.
     * @param expired the ClearAlarmExpirable
     * @return the removed element.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean removeClearAlarm(final ClearAlarmExpirable expired) {
        return this.clearAlarmsCacheManager.removeClearAlarm(expired);
    }

    /**
     * getCorrelatedAlarm method.
     * @param alarmRecord the ProcessedAlarmEvent.
     * @return the correlated alarm object.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public ProcessedAlarmEvent getCorrelatedAlarm(final ProcessedAlarmEvent alarmRecord) {
        return this.alarmCorrelator.getCorrelatedAlarm(alarmRecord);
    }
}
