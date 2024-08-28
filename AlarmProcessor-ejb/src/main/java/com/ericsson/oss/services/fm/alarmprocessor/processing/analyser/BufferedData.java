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

package com.ericsson.oss.services.fm.alarmprocessor.processing.analyser;

import java.util.ArrayList;
import java.util.List;

import com.ericsson.oss.mediation.translator.model.EventNotification;

/**
 * This is a POJO containing EventNotifications, and timestamp of which these events are added to the buffer.
 */
public class BufferedData {

    private long timeStamp;
    private boolean sentToNorthBound;
    private List<EventNotification> events = new ArrayList<>();

    public BufferedData(final long timeStamp, final boolean sentToNorthBound, final List<EventNotification> events) {
        this.timeStamp = timeStamp;
        this.sentToNorthBound = sentToNorthBound;
        this.events = events;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(final long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public List<EventNotification> getEvents() {
        return events;
    }

    public void setEvents(final List<EventNotification> events) {
        this.events = events;
    }

    public boolean isSentToNorthBound() {
        return sentToNorthBound;
    }

    public void setSentToNorthBound(final boolean sentToNorthBound) {
        this.sentToNorthBound = sentToNorthBound;
    }

}
