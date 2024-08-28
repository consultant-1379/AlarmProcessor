package com.ericsson.oss.services.fm.alarmprocessor.integration.test.util;

import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.fm.services.alarmsupervisioncontroller.api.AlarmSupervisionController;
import com.ericsson.oss.services.fm.services.alarmsupervisioncontroller.api.FmCliCommand;
import com.ericsson.oss.services.fm.services.alarmsupervisioncontroller.api.NodeSupervisionResponse;

@Stateless
public class AlarmSupervisionControllerImpl implements AlarmSupervisionController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlarmSupervisionControllerImpl.class);

    @Inject
    TestChecker testChecker;

    @Override
    public void initiateNetworkSync() {
    }

    @Override
    public List<NodeSupervisionResponse> initiateSync(final FmCliCommand cliCommand) {
        LOGGER.info("initiateSync {}", cliCommand);
        cliCommand.getSearchCriteria().forEach(nsc -> testChecker.addSyncCall(nsc.getNodeFdn()));
        return null;
    }

    @Override
    public void initiateSync(final FmCliCommand cliCommand, final boolean isRbacRequired) {
        LOGGER.info("initiateSync {} {}", cliCommand, isRbacRequired);
        cliCommand.getSearchCriteria().forEach(nsc -> testChecker.addSyncCall(nsc.getNodeFdn()));
    }

    @Override
    public List<NodeSupervisionResponse> setAutoSyncFlag(final FmCliCommand arg0) {
        return null;
    }

    @Override
    public List<NodeSupervisionResponse> setHBTimeoutValue(final FmCliCommand arg0) {
        return null;
    }

    @Override
    public List<NodeSupervisionResponse> setSupervisionState(final FmCliCommand arg0) {
        return null;
    }

    @Override
    public void syncAllSuppressedNodes() {
    }

    @Override
    public List<NodeSupervisionResponse> setHBIntervalValue(final FmCliCommand arg0) {
        return null;
    }

}
