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

package com.ericsson.oss.services.fm.alarmprocessor.instrumentation;

import static com.ericsson.oss.itpf.sdk.instrument.annotation.MonitoredAttribute.Category.THROUGHPUT;
import static com.ericsson.oss.itpf.sdk.instrument.annotation.MonitoredAttribute.Category.UTILIZATION;
import static com.ericsson.oss.itpf.sdk.instrument.annotation.MonitoredAttribute.CollectionType.DYNAMIC;
import static com.ericsson.oss.itpf.sdk.instrument.annotation.MonitoredAttribute.CollectionType.TRENDSUP;
import static com.ericsson.oss.itpf.sdk.instrument.annotation.MonitoredAttribute.Interval.ONE_MIN;
import static com.ericsson.oss.itpf.sdk.instrument.annotation.MonitoredAttribute.Units.NONE;
import static com.ericsson.oss.itpf.sdk.instrument.annotation.MonitoredAttribute.Visibility.ALL;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.instrument.annotation.InstrumentedBean;
import com.ericsson.oss.itpf.sdk.instrument.annotation.MonitoredAttribute;
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.fm.ratedetectionengine.api.ThresholdCrossed;

/**
 * Bean responsible for Instrumenting AlarmOverloadProtection statistics. It profiles Alarms Discarded, Alerts Discarded, Nodes Suppressed.
 */

@InstrumentedBean(displayName = "Alarm Overload Protection Metrics", description = "Alarm Overload Protection Records")
@ApplicationScoped
@Profiled
public class AOPInstrumentedBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(AOPInstrumentedBean.class);

    private int alarmsDiscarded;

    private int alertsDiscarded;

    private int nodesSuppressed;

    @Inject
    private SystemRecorder systemRecorder;

    public AOPInstrumentedBean() {
        alarmsDiscarded = 0;
        alertsDiscarded = 0;
        nodesSuppressed = 0;
    }

    @MonitoredAttribute(displayName = "Number of Alarms Discarded by APS", visibility = ALL, units = NONE,
                        category = THROUGHPUT, interval = ONE_MIN, collectionType = TRENDSUP)
    public int getAlarmCountDiscardedByAPS() {
        return alarmsDiscarded;
    }

    @MonitoredAttribute(displayName = "Number of Alerts Discarded by APS", visibility = ALL, units = NONE,
                        category = THROUGHPUT, interval = ONE_MIN, collectionType = TRENDSUP)
    public int getAlertCountDiscardedByAPS() {
        return alertsDiscarded;
    }

    @MonitoredAttribute(displayName = "Number of Nodes Suppressed by APS", visibility = ALL, units = NONE,
                        category = UTILIZATION, interval = ONE_MIN, collectionType = DYNAMIC)
    public int getNodeCountSuppressedByAPS() {
        return nodesSuppressed;
    }

    public void increaseAlarmCountDiscardedByAPS() {
        alarmsDiscarded++;
        LOGGER.debug("alarmCountDiscardedByAPS {} ", alarmsDiscarded);
    }

    public void increaseAlertCountDiscardedByAPS() {
        alertsDiscarded++;
        LOGGER.debug("alertCountDiscardedByAPS {} ", alertsDiscarded);
    }

    public void increaseNodeCountSuppressedByAPS() {
        nodesSuppressed++;
        LOGGER.debug("nodeCountSuppressedByAPS {} ", nodesSuppressed);
    }

    public void monitorSafeMode(final ThresholdCrossed safeMode) {
        final Map<String, Object> eventData = new HashMap<>();
        eventData.put("OVERLOAD", safeMode);
        systemRecorder.recordEventData("ALARM_OVERLOAD_PROTECTION", eventData);
        nodesSuppressed = 0;
    }
}
