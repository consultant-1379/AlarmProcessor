/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.fm.alarmprocessor.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.mediation.translator.model.EventNotification;
import com.ericsson.oss.mediation.translator.model.EventNotificationBatch;

/**
 * Utility class to serialize the given List of EventNotification events to EventNotificationBatch object.
 */
public final class EventNotificationSerializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventNotificationSerializer.class);

    private EventNotificationSerializer() {
    }

    /**
     * Serialize the given List of EventNotification events to EventNotificationBatch object.
     *
     * @param {@link List} eventNotifications
     * @return EventNotificationBatch
     */
    public static EventNotificationBatch serializeObject(final List<EventNotification> eventNotifications) {
         final EventNotificationBatch serializableEventnotification = new EventNotificationBatch();
        byte[] bytes = null;
        try(ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                ObjectOutput objectOutput = new ObjectOutputStream(byteArrayOutputStream)) {
            objectOutput.writeObject(eventNotifications);
            bytes = byteArrayOutputStream.toByteArray();
        } catch (final IOException ioException) {
            LOGGER.error("IO Exception during writeObject {}", ioException.getMessage());
            LOGGER.debug("IO Exception during writeObject : ", ioException);
        }
        serializableEventnotification.setSerializedData(bytes);

        return serializableEventnotification;
    }
}
