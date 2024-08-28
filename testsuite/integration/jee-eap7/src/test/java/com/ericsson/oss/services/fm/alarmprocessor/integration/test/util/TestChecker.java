package com.ericsson.oss.services.fm.alarmprocessor.integration.test.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class TestChecker {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestChecker.class);


    private CountDownLatch LATCH = new CountDownLatch(1);

    private final List<String> syncCalls = new ArrayList<>();

    public void resetLatch(final int numberOfSync) {
        this.LATCH = null;
        this.LATCH = new CountDownLatch(numberOfSync);
    }

    public void clearSyncCalls() {
        this.syncCalls.clear();
    }

    public boolean await(final long timeout, final TimeUnit unit) throws InterruptedException {
        return this.LATCH.await(timeout, unit);
    }

    public void addSyncCall(final String fdn) {
        LOGGER.info("Sync called on node {}", fdn);
        this.LATCH.countDown();
        this.syncCalls.add(fdn);
    }

    public List<String> getSyncCalls() {
        return this.syncCalls;
    }
}
