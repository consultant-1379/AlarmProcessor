/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2013
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.fm.alarmprocessor.integration.test.util;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.ejb.Singleton;
import javax.enterprise.event.Observes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.eventbus.model.annotation.Modeled;
import com.ericsson.oss.services.fm.models.processedevent.FMProcessedEventType;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;

@Singleton
public class FMAlarmOutBusTopicListener {

    public CountDownLatch LATCH;

    private final List<ProcessedAlarmEvent> receivedMessages = new LinkedList<>();

    private final List<ProcessedAlarmEvent> correlatedMessages = new LinkedList<>();

    private static final Logger LOGGER = LoggerFactory.getLogger(FMAlarmOutBusTopicListener.class);

    public void receiveAlarm(@Observes @Modeled final ProcessedAlarmEvent alarm) {
        LOGGER.info("Received alarm  {}", alarm);
        if ((alarm.getRecordType().equals(FMProcessedEventType.CLEARALL)) || (alarm.getEventPOId() > 0)) {
            this.receivedMessages.add(alarm);
            this.LATCH.countDown();
        } else {
            LOGGER.info("Alarm is not inserted in Versant");
        }

    }

    public FMAlarmOutBusTopicListener() {
        this.LATCH = new CountDownLatch(1);
    }

    public List<ProcessedAlarmEvent> getReceivedMessages() {
        return this.receivedMessages;
    }


    public List<ProcessedAlarmEvent> getCorrelatedMessages() {
        return this.correlatedMessages;
    }

    public void clear() {
        this.receivedMessages.clear();
        this.correlatedMessages.clear();
    }

    public long getCountInLatch() {
        return this.LATCH.getCount();
    }

    public void resetLatch() {
        this.LATCH = null;
        this.LATCH = new CountDownLatch(1);
    }

    public void resetLatch(final int numberOfAlarms) {
        this.LATCH = null;
        this.LATCH = new CountDownLatch(numberOfAlarms);
    }

}
