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

package com.ericsson.oss.services.fm.alarmprocessor.api.custom.attributes;

import java.util.Map;

import javax.ejb.Remote;

import com.ericsson.oss.itpf.sdk.core.annotation.EService;

/**
 * Interface that provides contract for Custom Attribute cache updates for FMX CREATED alarms.
 */
@EService
@Remote
public interface CustomAttributesHandler {

	void updateCustomAttributes(final Map<String, String> newAdditionalAttributes, final Long poId, Map<String, String> existingAdditionalAttributes);

}
