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

/**
 * Class is responsible for creating nodeFdn object for each Fdn.
 */
public class NodeRef {

    private String nodeFdn;

    public String getNodeFdn() {
        return nodeFdn;
    }

    public void setNodeFdn(final String nodeFdn) {
        this.nodeFdn = nodeFdn;
    }

    public static NodeRef fromNodeFdn(final String nodeFdn) {
        final NodeRef nodeRef = new NodeRef();
        nodeRef.setNodeFdn(nodeFdn);
        return nodeRef;
    }
}