package org.printscript.common;

public interface PrintScriptIterator<T> {

    boolean hasNext();

    T getNext();
}
