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

package com.ericsson.oss.services.fm.alarmprocessor.builders;

import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.NE_TYPE;

import com.ericsson.oss.services.fm.models.processedevent.AlarmMetadataInformation;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;

/**
 * Class builds AlarmMetadataInformation from ProcessedAlarmEvent.
 */
public class MetaDataInformationBuilder {

    /**
     * Method prepares {@link AlarmMetadataInformation} from {@link ProcessedAlarmEvent} to be sent to AlarmMetaDataChannel.
     * @param event
     *            FM processed alarm event received
     * @return {@link AlarmMetadataInformation}
     */
    public AlarmMetadataInformation build(final ProcessedAlarmEvent event) {
        final AlarmMetadataInformation metaDataInformation = new AlarmMetadataInformation();
        metaDataInformation.setSpecificProblem(event.getSpecificProblem());
        metaDataInformation.setProbableCause(event.getProbableCause());
        metaDataInformation.setEventType(event.getEventType());
        metaDataInformation.setNeType(event.getAdditionalInformation().get(NE_TYPE));
        return metaDataInformation;
    }
}