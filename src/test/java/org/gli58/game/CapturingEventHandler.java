package org.gli58.game;

import org.gli58.game.event.Event;
import org.gli58.game.event.EventHandler;

import java.util.HashSet;
import java.util.Set;

class CapturingEventHandler implements EventHandler {
    private Set<String> events = new HashSet<>();

    @Override
    public void handle(Event event) {
        events.add(event.getAsString());
    }

    public Set<String> getEventStrings() {
        return events;
    }
};
