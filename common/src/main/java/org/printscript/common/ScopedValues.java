package org.printscript.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Immutable stack of named values. Names in inner scopes shadow outer scopes. */
public final class ScopedValues<T> {

    private static final int OUTERMOST_FRAME_COUNT = 1;

    private final List<Map<String, T>> frames;

    public ScopedValues() {
        this(List.of(Map.of()));
    }

    private ScopedValues(List<Map<String, T>> frames) {
        this.frames = frames;
    }

    public ScopedValues<T> openScope() {
        List<Map<String, T>> opened = new ArrayList<>(frames);
        opened.add(Map.of());
        return new ScopedValues<>(List.copyOf(opened));
    }

    public ScopedValues<T> closeScope() {
        if (frames.size() == OUTERMOST_FRAME_COUNT) {
            return this;
        }
        return new ScopedValues<>(List.copyOf(frames.subList(0, frames.size() - 1)));
    }

    public Optional<ScopedValues<T>> declare(String name, T value) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(value, "value");
        if (frames.getLast().containsKey(name)) {
            return Optional.empty();
        }
        return Optional.of(replaceInFrame(frames.size() - 1, name, value));
    }

    public Optional<ScopedValues<T>> replace(String name, T value) {
        Objects.requireNonNull(value, "value");
        int frame = frameOf(name);
        if (frame < 0) {
            return Optional.empty();
        }
        return Optional.of(replaceInFrame(frame, name, value));
    }

    public Optional<T> read(String name) {
        int frame = frameOf(name);
        return frame < 0 ? Optional.empty() : Optional.of(frames.get(frame).get(name));
    }

    private int frameOf(String name) {
        Objects.requireNonNull(name, "name");
        for (int index = frames.size() - 1; index >= 0; index--) {
            if (frames.get(index).containsKey(name)) {
                return index;
            }
        }
        return -1;
    }

    private ScopedValues<T> replaceInFrame(int frame, String name, T value) {
        Map<String, T> updated = new HashMap<>(frames.get(frame));
        updated.put(name, value);
        List<Map<String, T>> replaced = new ArrayList<>(frames);
        replaced.set(frame, Map.copyOf(updated));
        return new ScopedValues<>(List.copyOf(replaced));
    }
}
