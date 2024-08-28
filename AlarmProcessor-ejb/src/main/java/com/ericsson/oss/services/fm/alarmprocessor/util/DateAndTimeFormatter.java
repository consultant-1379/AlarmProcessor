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

import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.DATE_FORMAT;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.SIMPLE_DATE_FORMAT;
import static com.ericsson.oss.services.fm.common.constants.GeneralConstants.DOT_DELIMITER;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class formats time in string format to date.
 */
public final class DateAndTimeFormatter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DateAndTimeFormatter.class);

    /**
     * Default Private Constructor.
     */
    private DateAndTimeFormatter() {
    }

    // TODO: Change once snmp Mediation sends the time in expected format.
    /**
     * Parse time to Date with the received attributes in alarm. time with milliseconds precision and timeZone are used to parse.
     *
     * @param String
     *            time
     * @param String
     *            timeZone
     * @param String
     *            timeType
     * @return Date
     */
    public static Date parseTime(final String time, final String timeZone) {
        try {
            final SimpleDateFormat simpleDateFormat;
            if (time.contains(DOT_DELIMITER)) {
                simpleDateFormat = new SimpleDateFormat(DATE_FORMAT);
            } else {
                simpleDateFormat = new SimpleDateFormat(SIMPLE_DATE_FORMAT);
            }
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone(TimeZone.getDefault().getID()));
            if (timeZone != null && !timeZone.isEmpty()) {
                simpleDateFormat.setTimeZone(TimeZone.getTimeZone(timeZone));
            }
            final Date date = simpleDateFormat.parse(time);
            LOGGER.debug("Time in MilliSec is:{}, timeZone is:{}", date.getTime(), timeZone);
            return date;
        } catch (final Exception exception) {
            LOGGER.warn("Exception in conversion of time {} and timeZone {} is :{}.", time, timeZone, exception.getMessage());
            LOGGER.debug("Exception in conversion of time {} and timeZone {} is :{}.", time, timeZone, exception);
            return new Date();
        }
    }
}
