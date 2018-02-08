package org.gli58.event;

/**
 * The event handling mechanism can be made async, through messaging channel, etc.
 *
 * Having this interface here to imply those flexibilities. handler can also have the
 * ability to subscribe certain type of events etc
 *
 */
public interface EventHandler {
    void handle(Event event);
}
