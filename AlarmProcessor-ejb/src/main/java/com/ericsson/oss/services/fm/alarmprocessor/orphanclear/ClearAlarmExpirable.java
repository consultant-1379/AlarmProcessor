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

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;

/**
 * The Clear Alarm Expirable class.
 */
public class ClearAlarmExpirable implements Delayed {
    private final Long timeout;
    private ProcessedAlarmEvent alarm;
    private final Comparator<ClearAlarmExpirable> comparator = Comparator.comparingLong(ClearAlarmExpirable::getTimeout);
    private ProcessedAlarmEvent correlatedAlarm;

    /**
     * CTOR.
     * @param alarm the ProcessedAlarmEvent.
     * @param timeout the timeout in millisec.
     */
    public ClearAlarmExpirable(final ProcessedAlarmEvent alarm, final Long timeout) {
        this.timeout = timeout;
        this.alarm = alarm;
    }

    /**
     * getTimeout.
     * @return the timeout in millisec.
     */
    public Long getTimeout() {
        return timeout;
    }

    /**
     * getAlarm.
     * @return the ProcessedAlarmEvent.
     */
    public ProcessedAlarmEvent getAlarm() {
        return alarm;
    }

    /**
     * Comparator.
     * @return the comparator.
     */
    public Comparator<ClearAlarmExpirable> getComparator() {
        return comparator;
    }

    /**
     * hasCorrelatedAlarm.
     * @return true if has correlated alarm or false if not.
     */
    public boolean hasCorrelatedAlarm() { return correlatedAlarm != null; }

    /**
     * getCorrelatedAlarm.
     * @return the correlated alarm object.
     */
    public ProcessedAlarmEvent getCorrelatedAlarm() { return correlatedAlarm; }

    /**
     * setCorrelatedAlarm.
     * @param alarm the given alarm object.
     */
    public void setCorrelatedAlarm(final ProcessedAlarmEvent alarm) { correlatedAlarm = alarm; }

    /**
     * compareTo.
     * @param other the given Delayed object.
     * @return the result of comparison between the given Delayed object timeout and the contained ones.
     */
    @Override
    public int compareTo(final Delayed other) {
        return (int) (getDelay(TimeUnit.MILLISECONDS) - other.getDelay(TimeUnit.MILLISECONDS));
    }

    /**
     * getDelay.
     * @param unit
     * @return the delay.
     */
    @Override
    public long getDelay(final TimeUnit unit) {
        return unit.convert(timeout - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * toString.
     * @return convert to string.
     */
    @Override
    public String toString() {
        return "ClearAlarmExpirable [timeout=" + getTimeStamp(timeout) + ", alarm=" + alarm + "]";
    }

    /**
     * setProcessedAlarm.
     * @param newElement set the given ProcessedAlarmEvent.
     */
    public void setProcessedAlarm(final ProcessedAlarmEvent newElement) {
        this.alarm = newElement;
    }

    private String getTimeStamp(final Long time) {
        return new SimpleDateFormat("HH:mm:ss.SSS").format(new Date(time));
    }

}
