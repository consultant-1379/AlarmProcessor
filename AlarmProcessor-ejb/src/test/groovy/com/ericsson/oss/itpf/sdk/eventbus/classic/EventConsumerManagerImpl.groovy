package com.ericsson.oss.itpf.sdk.eventbus.classic;

public class EventConsumerManagerImpl implements EventConsumerManagerSPI {

    @Override
    public boolean startListening(String destinationURI, EMessageListener listener, String filter) {
        return false;
    }

    @Override
    public boolean stopListening(String destinationURI, EMessageListener listener, String filter) {
        return false;
    }
}