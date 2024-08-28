/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.fm.alarmprocessor.fmdbavailability;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;

@RunWith(MockitoJUnitRunner.class)
public class DpsAvailabilityCallBackImplTest {

	@InjectMocks
	private DpsAvailabilityCallBackImpl dpsAvailabilityCallBackImpl;

	@Mock
	private DatabaseStatusProcessor databaseStatusProcessor;

	@Mock
	private SystemRecorder systemRecorder;

	@Mock
	private Logger logger;

	@Before
	public void setup() {
          MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testOnServiceAvailable() {
          dpsAvailabilityCallBackImpl.onServiceAvailable();
          verify(databaseStatusProcessor, times(1)).updateDatabaseStatusToAvailable();
	}

	@Test
	public void testOnServiceUnavailable() {
          dpsAvailabilityCallBackImpl.onServiceUnavailable();
          verify(databaseStatusProcessor, times(1)).updateDatabaseStatusToFailed();
	}

}
