package org.gli58.game.event;

public class ConsoleLoggingEventHandler implements EventHandler {
    @Override
    public void handle(Event event) {
        System.out.println(event.getAsString());
    }
}
