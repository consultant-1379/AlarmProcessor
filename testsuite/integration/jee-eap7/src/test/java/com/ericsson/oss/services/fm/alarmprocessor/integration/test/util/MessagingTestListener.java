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

public abstract class MessagingTestListener {

	public CountDownLatch LATCH;

	private final List<Object> receivedMessages = new LinkedList<>();

	void initCountDown(final int count) {
		LATCH = new CountDownLatch(count);
	}

	public void mark(final Object msg) {
		receivedMessages.add(msg);
		LATCH.countDown();
		System.out.println("Marked message!!");
	}

	public List<Object> getReceivedMessages() {
		return receivedMessages;
	}

	public void clear(final int count) {
		receivedMessages.clear();
		LATCH = new CountDownLatch(count);
	}
	
}
