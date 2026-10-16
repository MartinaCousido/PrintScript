package org.printscript.common;

import java.util.Optional;

/** Immutable AVL tree: updates copy only a balanced search path. */
final class PersistentNames<T> {
    private static final int MAX_BALANCE = 1;
    private final Node<T> root;

    PersistentNames() {
        this(null);
    }

    private PersistentNames(Node<T> root) {
        this.root = root;
    }

    Optional<T> read(String name) {
        Node<T> current = root;
        while (current != null) {
            int comparison = name.compareTo(current.name);
            if (comparison == 0) {
                return Optional.of(current.value);
            }
            current = comparison < 0 ? current.left : current.right;
        }
        return Optional.empty();
    }

    PersistentNames<T> put(String name, T value) {
        return new PersistentNames<>(put(root, name, value));
    }

    // AVL height bounds this recursion to O(log n), irrespective of source nesting.
    private static <T> Node<T> put(Node<T> node, String name, T value) {
        if (node == null) {
            return new Node<>(name, value, null, null);
        }
        int comparison = name.compareTo(node.name);
        if (comparison == 0) {
            return new Node<>(name, value, node.left, node.right);
        }
        Node<T> updated =
                comparison < 0
                        ? new Node<>(node.name, node.value, put(node.left, name, value), node.right)
                        : new Node<>(
                                node.name, node.value, node.left, put(node.right, name, value));
        return balance(updated);
    }

    private static <T> Node<T> balance(Node<T> node) {
        int difference = height(node.left) - height(node.right);
        if (difference > MAX_BALANCE) {
            Node<T> left = node.left;
            if (height(left.left) < height(left.right)) {
                left = rotateLeft(left);
            }
            return rotateRight(new Node<>(node.name, node.value, left, node.right));
        }
        if (difference < -MAX_BALANCE) {
            Node<T> right = node.right;
            if (height(right.right) < height(right.left)) {
                right = rotateRight(right);
            }
            return rotateLeft(new Node<>(node.name, node.value, node.left, right));
        }
        return node;
    }

    private static <T> Node<T> rotateLeft(Node<T> node) {
        Node<T> pivot = node.right;
        Node<T> moved = new Node<>(node.name, node.value, node.left, pivot.left);
        return new Node<>(pivot.name, pivot.value, moved, pivot.right);
    }

    private static <T> Node<T> rotateRight(Node<T> node) {
        Node<T> pivot = node.left;
        Node<T> moved = new Node<>(node.name, node.value, pivot.right, node.right);
        return new Node<>(pivot.name, pivot.value, pivot.left, moved);
    }

    private static int height(Node<?> node) {
        return node == null ? 0 : node.height;
    }

    private static final class Node<T> {
        private final String name;
        private final T value;
        private final Node<T> left;
        private final Node<T> right;
        private final int height;

        private Node(String name, T value, Node<T> left, Node<T> right) {
            this.name = name;
            this.value = value;
            this.left = left;
            this.right = right;
            this.height = Math.max(height(left), height(right)) + 1;
        }
    }
}
