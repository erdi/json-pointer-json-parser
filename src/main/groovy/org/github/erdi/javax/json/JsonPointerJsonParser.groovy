package org.github.erdi.javax.json


import groovy.transform.InheritConstructors

import javax.json.stream.JsonParser

class JsonPointerJsonParser implements JsonParser {

    private final LinkedList<JsonPointerState> states = [new InitialState()] as LinkedList

    private Event currentEvent

    @Delegate
    JsonParser backing

    JsonPointerJsonParser(JsonParser backing) {
        this.backing = backing
    }

    @Override
    Event next() {
        currentEvent = backing.next()
        states.last.handle(currentEvent)
        currentEvent
    }

    @Override
    void skipObject() {
        backing.skipObject()
        if (currentEvent == javax.json.stream.JsonParser.Event.START_OBJECT) {
            states.last.handle(javax.json.stream.JsonParser.Event.END_OBJECT)
        }
    }

    @Override
    void skipArray() {
        backing.skipArray()
        if (currentEvent == javax.json.stream.JsonParser.Event.START_ARRAY) {
            states.last.handle(javax.json.stream.JsonParser.Event.END_ARRAY)
        }
    }

    String getPointer() {
        states*.toPointer().join("")
    }

    private interface JsonPointerState {
        void handle(Event event)

        String toPointer()
    }

    private class InitialState implements JsonPointerState {
        @Override
        void handle(Event event) {
            switch (event) {
                case javax.json.stream.JsonParser.Event.START_OBJECT:
                    states << new RootObjectState()
                    break
                case javax.json.stream.JsonParser.Event.START_ARRAY:
                    states << new RootArrayState()
                    break
                default:
                    throw new IllegalArgumentException("Unexpected event: ${event.name()}")
            }
        }

        @Override
        String toPointer() {
            ""
        }
    }

    private class KeyState implements JsonPointerState {
        private final String key

        KeyState(String key) {
            this.key = key
        }

        @Override
        void handle(Event event) {
            switch (event) {
                case javax.json.stream.JsonParser.Event.VALUE_STRING:
                case javax.json.stream.JsonParser.Event.VALUE_NUMBER:
                case javax.json.stream.JsonParser.Event.VALUE_FALSE:
                case javax.json.stream.JsonParser.Event.VALUE_TRUE:
                case javax.json.stream.JsonParser.Event.VALUE_NULL:
                    nextState(new ValueState(key))
                    break
                case javax.json.stream.JsonParser.Event.START_OBJECT:
                    nextState(new ObjectState(key))
                    break
                case javax.json.stream.JsonParser.Event.START_ARRAY:
                    nextState(new ArrayState(key))
                    break
                default:
                    throw new IllegalArgumentException("Unexpected event: ${event.name()}")
            }
        }

        private void nextState(JsonPointerState state) {
            states.removeLast()
            states << state
        }

        @Override
        String toPointer() {
            ""
        }
    }

    private abstract class AbstractKeyedState implements JsonPointerState {
        private final String key

        AbstractKeyedState(String key) {
            this.key = key
        }

        @Override
        String toPointer() {
            "/$key"
        }
    }

    private abstract class AbstractRootState implements JsonPointerState {
        protected boolean isLastNonEmptyPointerState() {
            states.size() == 2 || (states.size() == 3 && states.last.toPointer().empty)
        }

        @Override
        String toPointer() {
            lastNonEmptyPointerState ? "/" : ""
        }
    }

    private class ArrayStateHandler {
        private int index = 0

        void handle(Event event) {
            switch (event) {
                case javax.json.stream.JsonParser.Event.VALUE_STRING:
                case javax.json.stream.JsonParser.Event.VALUE_NUMBER:
                case javax.json.stream.JsonParser.Event.VALUE_FALSE:
                case javax.json.stream.JsonParser.Event.VALUE_TRUE:
                case javax.json.stream.JsonParser.Event.VALUE_NULL:
                    nextState(new ValueState(index.toString()))
                    break
                case javax.json.stream.JsonParser.Event.START_ARRAY:
                    nextState(new ArrayState(index.toString()))
                    break
                case javax.json.stream.JsonParser.Event.START_OBJECT:
                    nextState(new ObjectState(index.toString()))
                    break
                case javax.json.stream.JsonParser.Event.END_ARRAY:
                    states.removeLast()
                    break
                default:
                    throw new IllegalArgumentException("Unexpected event: ${event.name()}")
            }
        }

        private void nextState(JsonPointerState state) {
            states << state
            index++
        }
    }

    private class RootArrayState extends AbstractRootState {
        @Delegate
        private final ArrayStateHandler arrayStateHandler = new ArrayStateHandler()
    }

    @InheritConstructors
    private class ArrayState extends AbstractKeyedState {
        @Delegate
        private final ArrayStateHandler arrayStateHandler = new ArrayStateHandler()
    }

    private class ObjectStateHandler {
        void handle(Event event) {
            switch (event) {
                case javax.json.stream.JsonParser.Event.KEY_NAME:
                    states << new KeyState(string)
                    break
                case javax.json.stream.JsonParser.Event.END_OBJECT:
                    states.removeLast()
                    break
                default:
                    throw new IllegalArgumentException("Unexpected event: ${event.name()}")
            }
        }
    }

    private class RootObjectState extends AbstractRootState {
        @Delegate
        private final ObjectStateHandler handler = new ObjectStateHandler()
    }

    @InheritConstructors
    private class ObjectState extends AbstractKeyedState {
        @Delegate
        private final ObjectStateHandler handler = new ObjectStateHandler()
    }

    @InheritConstructors
    private class ValueState extends AbstractKeyedState {
        @Override
        void handle(Event event) {
            states.removeLast()
            states.last.handle(event)
        }
    }
}
