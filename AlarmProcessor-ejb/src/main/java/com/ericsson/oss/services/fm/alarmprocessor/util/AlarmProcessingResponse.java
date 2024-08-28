/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.fm.alarmprocessor.util;

import java.util.ArrayList;
import java.util.List;

import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;

/**
 * This object consists boolean fields for initiating alarm synchronization , retry for alarm processing , to send fake clear to NorthBound ,
 * processedAlarms list to be sent out to FMAlarmOutTopic.
 */
public class AlarmProcessingResponse {

    // Flag to indicate if alarm processing is to be retried.
    private boolean retryFlag;

    // List of processed Alarms to be sent to Topic
    private List<ProcessedAlarmEvent> processedAlarms;

    // Flag to indicate if alarm synchronization is to be initiated.
    private boolean initiateAlarmSync;

    // Flag to indicate if fake Clear alarms is to be sent to NorthBound.
    private boolean sendFakeClearToNbi;

    // Flag to indicate if fake Clear alarms is to be sent to UI.
    private boolean sendFakeClearToUiAndNbi;

    public AlarmProcessingResponse() {
        retryFlag = false;
        processedAlarms = new ArrayList<ProcessedAlarmEvent>(8);
        initiateAlarmSync = false;
        sendFakeClearToNbi = false;
        sendFakeClearToUiAndNbi = false;
    }

    public boolean isRetryFlag() {
        return retryFlag;
    }

    public void setRetryFlag(final boolean retryFlag) {
        this.retryFlag = retryFlag;
    }

    public List<ProcessedAlarmEvent> getProcessedAlarms() {
        return processedAlarms;
    }

    public void setProcessedAlarms(final List<ProcessedAlarmEvent> processedAlarms) {
        this.processedAlarms = processedAlarms;
    }

    public boolean isInitiateAlarmSync() {
        return initiateAlarmSync;
    }

    public void setInitiateAlarmSync(final boolean initiateAlarmSync) {
        this.initiateAlarmSync = initiateAlarmSync;
    }

    public boolean isSendFakeClearToNbi() {
        return sendFakeClearToNbi;
    }

    public void setSendFakeClearToNbi(final boolean sendFakeClearToNbi) {
        this.sendFakeClearToNbi = sendFakeClearToNbi;
    }

    public boolean isSendFakeClearToUiAndNbi() {
        return sendFakeClearToUiAndNbi;
    }

    public void setSendFakeClearToUiAndNbi(final boolean sendFakeClearToUiAndNbi) {
        this.sendFakeClearToUiAndNbi = sendFakeClearToUiAndNbi;
    }
}
