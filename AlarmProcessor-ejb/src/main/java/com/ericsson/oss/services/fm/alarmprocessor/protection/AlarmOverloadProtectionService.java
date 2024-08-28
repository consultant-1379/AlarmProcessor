/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with writteLOGGER
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.fm.alarmprocessor.protection;

import static com.ericsson.oss.services.alarm.action.service.model.AlarmAction.ACK;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.ALARM_OVERLOAD_PROTECTION_ALERT_ATTEMPTS;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.ALARM_OVERLOAD_PROTECTION_ALERT_INTERVAL;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.ALARM_OVERLOAD_PROTECTION_ET;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.ALARM_OVERLOAD_PROTECTION_FDN;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.ALARM_OVERLOAD_PROTECTION_PC;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.ALARM_OVERLOAD_PROTECTION_PT;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.ALARM_OVERLOAD_PROTECTION_SP;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.ALARM_OVERLOAD_PROTECTION_START_TIMER_MSEC;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.ALARM_OVERLOAD_PROTECTION_SUPPRESSED;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.APS_SERVICE_ID;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.DEFAULT_ALARM_OVERLOAD_LOWER_PROTECTION_THRESHOLD;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.DEFAULT_ALARM_OVERLOAD_PROTECTION_THRESHOLD;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.RATE_DETECTION_SESSION_NAME;
import static com.ericsson.oss.services.fm.common.constants.AdditionalAttrConstants.ADDITIONAL_TEXT;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.EVENT_TYPE;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.FDN;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.OBJECT_OF_REFERENCE;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.PRESENT_SEVERITY;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.PROBABLE_CAUSE;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.RECORD_TYPE;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.SPECIFIC_PROBLEM;
import static com.ericsson.oss.services.fm.common.constants.ManagedObjectConstants.FM_SUPERVISEDOBJECT_SERVICE_STATE;
import static com.ericsson.oss.services.fm.models.processedevent.FMProcessedEventType.ERROR_MESSAGE;
import static com.ericsson.oss.services.fm.models.processedevent.ProcessedEventSeverity.CRITICAL;
import static com.ericsson.oss.services.fm.ratedetectionengine.api.ThresholdCrossed.OFF;
import static com.ericsson.oss.services.fm.ratedetectionengine.api.ThresholdCrossed.ON;
import static com.ericsson.oss.services.fm.ratedetectionengine.api.ThresholdCrossed.WARN;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.itpf.sdk.core.classic.ServiceFinderBean;
import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommand;
import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommandException;
import com.ericsson.oss.itpf.sdk.core.retry.RetryContext;
import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.itpf.sdk.core.retry.RetryPolicy;
import com.ericsson.oss.itpf.sdk.recording.ErrorSeverity;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.fm.alarmprocessor.cluster.MembershipChangeProcessor;
import com.ericsson.oss.services.fm.alarmprocessor.configuration.ConfigParametersListener;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.FmFunctionMoService;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.OpenAlarmService;
import com.ericsson.oss.services.fm.alarmprocessor.instrumentation.AOPInstrumentedBean;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmActionPerformer;
import com.ericsson.oss.services.fm.alarmprocessor.util.InternalAlarmGenerator;
import com.ericsson.oss.services.fm.ratedetectionengine.api.RateDetectionService;
import com.ericsson.oss.services.fm.ratedetectionengine.api.ThresholdAttributes;
import com.ericsson.oss.services.fm.ratedetectionengine.api.ThresholdCrossed;
import com.ericsson.oss.services.fm.services.alarmsupervisioncontroller.api.AlarmSupervisionController;

/**
 * This class it's used to start sessions on rate detection engine and configure thresholds parameters.
 */
@Singleton
@Startup
public class AlarmOverloadProtectionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlarmOverloadProtectionService.class);

    private ThresholdCrossed safeMode = OFF;

    @Inject
    private ConfigParametersListener configParametersListener;

    @Inject
    private OpenAlarmService openAlarmService;

    @Inject
    private InternalAlarmGenerator internalAlarmGenerator;

    @Inject
    private AlarmActionPerformer alarmActionPerformer;

    @Inject
    private RetryManager retryManager;

    @Inject
    private FmFunctionMoService fmFunctionMoService;

    @Inject
    private MembershipChangeProcessor membershipChangeProcessor;

    @Inject
    private SystemRecorder systemRecorder;

    @Inject
    private AOPInstrumentedBean aopInstrumentedBean;

    @EServiceRef
    private RateDetectionService rateDetectionService;

    @EServiceRef
    private AlarmSupervisionController alarmSupervisionController;

    @Resource
    private TimerService timerService;

    private Timer timer;

    /**
     * initTimer post construction.
     */
    @PostConstruct
    private void initTimer() {
        try {
            this.tryStartTimer();
        } catch (final Exception e) {
            LOGGER.error("Start timer for AlarmOverloadProtectionService: exception : ", e);
        }
    }

    /**
     * The timer callback.
     * @param t the Timer obejct.
     */
    @Timeout
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    @Lock(LockType.READ)
    public void onExpired(final Timer t) {
        this.timer = null;
        // Wait for the RateDetectionService has been deployed.
        if (this.getRateDetectionService() == null) {
            initTimer();
            return;
        }
        this.init();
    }

    /**
     * setAlarmOverloadProtection.
     * @param newValue the new value.
     */
    public void setAlarmOverloadProtection(final Boolean newValue) {
        if (!newValue) {
            rateDetectionService.resetSessionCounter(RATE_DETECTION_SESSION_NAME);
        }
    }

    /**
     * setRaisingThreshold.
     * @param newValue the new value.
     */
    public void setRaisingThreshold(final Long newValue) {
        final Integer alarmOverloadProtectionLowerThreshold = configParametersListener.getAlarmOverloadProtectionLowerThreshold();
        final ThresholdAttributes thresholdAttributes = new ThresholdAttributes.Builder().with(data -> {
            data.raiseThreshold = newValue;
            data.clearThresholdPercentage = alarmOverloadProtectionLowerThreshold;
        }).build();
        rateDetectionService.startDetectionSession(RATE_DETECTION_SESSION_NAME, thresholdAttributes);
    }

    /**
     * setWarningClearThresholdPercentage.
     * @param newValue the new value.
     */
    public void setWarningClearThresholdPercentage(final Integer newValue) {
        final Long alarmOverloadProtectionThreshold = configParametersListener.getAlarmOverloadProtectionThreshold();
        final ThresholdAttributes thresholdAttributes = new ThresholdAttributes.Builder().with(data -> {
            data.raiseThreshold = alarmOverloadProtectionThreshold;
            data.clearThresholdPercentage = newValue;
        }).build();
        rateDetectionService.startDetectionSession(RATE_DETECTION_SESSION_NAME, thresholdAttributes);
    }

    /**
     * getSafeMode.
     * @return the ThresholdCrossed object.
     */
    @Lock(LockType.READ)
    public ThresholdCrossed getSafeMode() {
        return safeMode;
    }

    /**
     * setSafeMode.
     * @param value the new ThresholdCrossed value.
     */
    public void setSafeMode(final ThresholdCrossed value) {
        safeMode = value;
        aopInstrumentedBean.monitorSafeMode(safeMode);
        if (membershipChangeProcessor.getMasterState()) {
            if (safeMode == ON) {
                tryRaiseThresholdCrossingAlert();
            } else if (safeMode == WARN) {
                tryAckThresholdCrossingAlert();
                syncAllSuppressedNodes();
            }
        }
    }

    /**
     * updateStateToAlarmSuppressed.
     * @param fdn the FDN.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @Lock(LockType.READ)
    public void updateStateToAlarmSuppressed(final String fdn) {
        final String currentServiceState = (String) fmFunctionMoService.read(fdn, FM_SUPERVISEDOBJECT_SERVICE_STATE);
        if (currentServiceState != null) {
            if (!ALARM_OVERLOAD_PROTECTION_SUPPRESSED.equals(currentServiceState)) {
                fmFunctionMoService.updateCurrentServiceState(fdn, ALARM_OVERLOAD_PROTECTION_SUPPRESSED);
                systemRecorder.recordEvent("APS", EventLevel.DETAILED, "currentServiceState",
                                "is changed to ALARM_OVERLOAD_PROTECTION_SUPPRESSED for fdn", fdn);
                aopInstrumentedBean.increaseNodeCountSuppressedByAPS();
            }
        } else {
            systemRecorder.recordError("APS", ErrorSeverity.CRITICAL, "currentServiceState", "is null for fdn.", fdn);
        }
    }

    private void tryRaiseThresholdCrossingAlert() {
        final RetryPolicy policy = getRetryPolicy(ALARM_OVERLOAD_PROTECTION_ALERT_ATTEMPTS, ALARM_OVERLOAD_PROTECTION_ALERT_INTERVAL);
        // creates and executes a retry-able command
        try {
            retryManager.executeCommand(policy, context -> {
                raiseThresholdCrossingAlert();
                return null;
            });
        } catch (final RetriableCommandException exception) {
            LOGGER.error("Failed to raise alam overload protection alert after {} attempts: {}", policy.getAttempts(), exception.getMessage());
        }
    }

    private void raiseThresholdCrossingAlert() {
        final Map<String, Object> alertDetails = new HashMap<>();
        final String fdn = ALARM_OVERLOAD_PROTECTION_FDN;
        alertDetails.put(EVENT_TYPE, ALARM_OVERLOAD_PROTECTION_ET);
        alertDetails.put(PROBABLE_CAUSE, ALARM_OVERLOAD_PROTECTION_PC);
        alertDetails.put(SPECIFIC_PROBLEM, ALARM_OVERLOAD_PROTECTION_SP);
        alertDetails.put(ADDITIONAL_TEXT, getAlarmOverloadProtectionProblemText());
        alertDetails.put(PRESENT_SEVERITY, CRITICAL.toString());
        alertDetails.put(RECORD_TYPE, ERROR_MESSAGE.toString());
        alertDetails.put(OBJECT_OF_REFERENCE, fdn);
        alertDetails.put(FDN, fdn);
        alertDetails.put(APS_SERVICE_ID, APS_SERVICE_ID);
        LOGGER.info("Threshold crossing alert sent: {}", alertDetails);
        internalAlarmGenerator.raiseInternalAlarm(alertDetails);
    }

    private void tryAckThresholdCrossingAlert() {
        final RetryPolicy policy = getRetryPolicy(ALARM_OVERLOAD_PROTECTION_ALERT_ATTEMPTS, ALARM_OVERLOAD_PROTECTION_ALERT_INTERVAL);
        // creates and executes a retry-able command
        try {
            retryManager.executeCommand(policy, context -> {
                ackThresholdCrossingAlert();
                return null;
            });
        } catch (final RetriableCommandException exception) {
            LOGGER.error("Failed to ack alam overload protection alert after {} attempts: {}", policy.getAttempts(), exception.getMessage());
        }
    }

    private void ackThresholdCrossingAlert() {
        final Map<String, Object> singleValuedAttributes = new HashMap<>();
        singleValuedAttributes.put(FDN, ALARM_OVERLOAD_PROTECTION_FDN);
        singleValuedAttributes.put(PRESENT_SEVERITY, CRITICAL.toString());
        singleValuedAttributes.put(SPECIFIC_PROBLEM, ALARM_OVERLOAD_PROTECTION_SP);
        singleValuedAttributes.put(PROBABLE_CAUSE, ALARM_OVERLOAD_PROTECTION_PC);
        final Iterator<PersistenceObject> poIterator =
                openAlarmService.getOpenAlarmPO(singleValuedAttributes, new HashMap<String, List<String>>());
        if (poIterator == null || !poIterator.hasNext()) {
            LOGGER.error("No Alert found about Overload Protection !!");
        } else {
            performAckThresholdCrossingAlert(poIterator.next());
        }
    }

    private void performAckThresholdCrossingAlert(final PersistenceObject po) {
        final List<Long> poIds = new ArrayList<>();
        poIds.add(po.getPoId());
        LOGGER.info("Threshold crossing alert ack: {}", poIds);
        alarmActionPerformer.performAction(poIds, ACK);
    }

    private RetryPolicy getRetryPolicy(final int attempt, final int interval) {
        return RetryPolicy.builder().attempts(attempt).waitInterval(interval, TimeUnit.SECONDS).retryOn(Exception.class).build();
    }

    private void syncAllSuppressedNodes() {
        LOGGER.info("Sync all suppressed nodes");
        alarmSupervisionController.syncAllSuppressedNodes();
    }

    private void tryStartTimer() {
        final RetryPolicy policy = RetryPolicy.builder().attempts(10).waitInterval(1, TimeUnit.SECONDS).retryOn(Exception.class).build();
        // creates and executes a retry-able command
        this.retryManager.executeCommand(policy, new RetriableCommand<Void>() {
            @Override
            public Void execute(final RetryContext retryContext) throws Exception {
                startTimer();
                return null;
            }
        });
    }

    private void init() {
        // Configure rate and start detection service
        final Long alarmOverloadProtectionThreshold = configParametersListener.getAlarmOverloadProtectionThreshold();
        final Integer alarmOverloadProtectionLowerThreshold = configParametersListener.getAlarmOverloadProtectionLowerThreshold();
        final ThresholdAttributes thresholdAttributes = new ThresholdAttributes.Builder().with(data -> {
            data.raiseThreshold = (alarmOverloadProtectionThreshold == null) ? DEFAULT_ALARM_OVERLOAD_PROTECTION_THRESHOLD :
                alarmOverloadProtectionThreshold;
            data.clearThresholdPercentage = (alarmOverloadProtectionLowerThreshold == null) ? DEFAULT_ALARM_OVERLOAD_LOWER_PROTECTION_THRESHOLD :
                alarmOverloadProtectionLowerThreshold;
        }).build();
        rateDetectionService.startDetectionSession(RATE_DETECTION_SESSION_NAME, thresholdAttributes);
    }

    /**
     * Create a single-action timer that expires after a specified duration.<br>
     *
     * @return The timer.
     * @throws OverloadProtectionServiceException  the exception.
     */
    private Timer startTimer() throws OverloadProtectionServiceException {
        if (this.timerService != null) {
            if (this.timer == null) {
                final TimerConfig timerConfig = new TimerConfig();
                timerConfig.setPersistent(false);
                this.timer = this.timerService.createSingleActionTimer(ALARM_OVERLOAD_PROTECTION_START_TIMER_MSEC, timerConfig);
                LOGGER.info("Start timer for AlarmOverloadProtectionService: it will expire in {} milliseconds",
                                ALARM_OVERLOAD_PROTECTION_START_TIMER_MSEC);
            }
        } else {
            LOGGER.error("Start timer for AlarmOverloadProtectionService: timer service unavailable");
            throw new OverloadProtectionServiceException("Startup failed: Timer service unavailable");
        }
        return this.timer;
    }

    /**
     * Retrieve the RateDetectionService, assign it to class attribute and return the service instance.
     * @return the RateDetectionService instance.
     */
    private RateDetectionService getRateDetectionService() {
        final ServiceFinderBean serviceFinder = new ServiceFinderBean();
        return serviceFinder.find(RateDetectionService.class);
    }

    /**
     * Retrieve the problem text of the alert about alarm overload protection.
     * @return the problem text string.
     */
    private String getAlarmOverloadProtectionProblemText() {
        final String currentThreshold = String.valueOf(configParametersListener.getAlarmOverloadProtectionThreshold());
        return String.format(ALARM_OVERLOAD_PROTECTION_PT, currentThreshold);
    }

}
