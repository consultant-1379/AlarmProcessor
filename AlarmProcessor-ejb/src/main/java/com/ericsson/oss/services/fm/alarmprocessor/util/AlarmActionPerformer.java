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

package com.ericsson.oss.services.fm.alarmprocessor.util;

import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.APS_SERVICE_ID;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.services.alarm.action.service.api.AlarmActionService;
import com.ericsson.oss.services.alarm.action.service.model.AlarmAction;
import com.ericsson.oss.services.alarm.action.service.model.AlarmActionData;

/**
 * This class performs alarm actions
 */
@ApplicationScoped
public class AlarmActionPerformer {

    @EServiceRef
    private AlarmActionService alarmActionService;

    /**
     * This method perform an alarm action.
     *
     * @param poIds
     *            poIds involved by the action
     * @param alarmAction
     *            action to be performed
     */
    public void performAction(final List<Long> poIds, final AlarmAction alarmAction) {
        final AlarmActionData alarmActionData = new AlarmActionData();
        alarmActionData.setOperatorName(APS_SERVICE_ID);
        alarmActionData.setAlarmIds(poIds);
        alarmActionData.setAction(alarmAction);
        alarmActionService.alarmActionUpdate(alarmActionData);
    }
}
