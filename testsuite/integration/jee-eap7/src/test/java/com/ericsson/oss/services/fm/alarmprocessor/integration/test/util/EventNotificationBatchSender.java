/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2012
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.fm.alarmprocessor.integration.test.util;

import javax.inject.Inject;

import com.ericsson.oss.itpf.sdk.eventbus.model.EventSender;
import com.ericsson.oss.itpf.sdk.eventbus.model.annotation.Modeled;
import com.ericsson.oss.mediation.translator.model.EventNotificationBatch;

public class EventNotificationBatchSender {

    @Inject
    @Modeled
    private EventSender<EventNotificationBatch> sender;

    public void sendEventNotificationBatch(final EventNotificationBatch eventNotificationBatch) {
        sender.send(eventNotificationBatch);
    }

}
