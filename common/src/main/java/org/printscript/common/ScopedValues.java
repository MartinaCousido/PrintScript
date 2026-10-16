package org.printscript.common;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.Optional;

/** Immutable lexical scopes with O(1) entry/exit and O(log n) writes within a frame. */
public final class ScopedValues<T> {
    private final Frame<T> current;

    public ScopedValues() {
        this(new Frame<>(new PersistentNames<>(), null));
    }

    private ScopedValues(Frame<T> current) {
        this.current = current;
    }

    public ScopedValues<T> openScope() {
        return new ScopedValues<>(new Frame<>(new PersistentNames<>(), current));
    }

    public ScopedValues<T> closeScope() {
        return current.parent == null ? this : new ScopedValues<>(current.parent);
    }

    public Optional<ScopedValues<T>> declare(String name, T value) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(value, "value");
        if (current.values.read(name).isPresent()) {
            return Optional.empty();
        }
        return Optional.of(
                new ScopedValues<>(new Frame<>(current.values.put(name, value), current.parent)));
    }

    public Optional<ScopedValues<T>> replace(String name, T value) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(value, "value");
        Deque<Frame<T>> inner = new ArrayDeque<>();
        Frame<T> target = current;
        while (target != null && target.values.read(name).isEmpty()) {
            inner.push(target);
            target = target.parent;
        }
        if (target == null) {
            return Optional.empty();
        }
        Frame<T> updated = new Frame<>(target.values.put(name, value), target.parent);
        while (!inner.isEmpty()) {
            Frame<T> frame = inner.pop();
            updated = new Frame<>(frame.values, updated);
        }
        return Optional.of(new ScopedValues<>(updated));
    }

    public Optional<T> read(String name) {
        Objects.requireNonNull(name, "name");
        for (Frame<T> frame = current; frame != null; frame = frame.parent) {
            Optional<T> value = frame.values.read(name);
            if (value.isPresent()) {
                return value;
            }
        }
        return Optional.empty();
    }

    private static final class Frame<T> {
        private final PersistentNames<T> values;
        private final Frame<T> parent;

        private Frame(PersistentNames<T> values, Frame<T> parent) {
            this.values = values;
            this.parent = parent;
        }
    }
}
