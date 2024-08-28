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

package com.ericsson.oss.services.fm.alarmprocessor.protection;

import javax.ejb.Stateless;
import javax.inject.Inject;

import com.ericsson.oss.services.fm.ratedetectionengine.api.ThresholdCrossed;
import com.ericsson.oss.services.fm.ratedetectionengine.api.ThresholdCrossing;

/**
 * This class implements the remote interface used on rate detection engine to change the safeMode when
 * thresholds are crossed.
 */
@Stateless
public class ThresholdCrossingImpl implements ThresholdCrossing {

    @Inject
    private AlarmOverloadProtectionService protectionService;

    @Override
    public void updateThresholdCrossed(final ThresholdCrossed value) {
        protectionService.setSafeMode(value);
    }

}
