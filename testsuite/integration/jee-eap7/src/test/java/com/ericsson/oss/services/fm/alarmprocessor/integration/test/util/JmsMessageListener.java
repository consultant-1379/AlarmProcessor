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

import javax.jms.Message;
import javax.jms.MessageListener;

public class JmsMessageListener extends MessagingTestListener implements MessageListener {

	public JmsMessageListener(int count){
		super.initCountDown(count);
	}
	@Override
	public void onMessage(Message msg) {
		System.out.println("Received Message"+msg);
		this.mark(msg);
	}

}
