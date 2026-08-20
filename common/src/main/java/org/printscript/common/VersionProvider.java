package org.printscript.common;

@FunctionalInterface
public interface VersionProvider<T> {

    T providerFor(String version);

    static IllegalArgumentException unknownVersion(String requested, String supported) {
        return new IllegalArgumentException(
                "Versión de PrintScript desconocida: "
                        + requested
                        + ". Versiones soportadas: "
                        + supported);
    }
}
