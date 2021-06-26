package io.github.karlatemp.jhf.launcher;

import java.util.Enumeration;
import java.util.Iterator;

class ItrEnumeration<T> implements Enumeration<T> {
    private final Iterator<T> itr;

    ItrEnumeration(Iterator<T> itr) {
        this.itr = itr;
    }

    @Override
    public boolean hasMoreElements() {
        return itr.hasNext();
    }

    @Override
    public T nextElement() {
        return itr.next();
    }

    public Iterator<T> asIterator() {
        return itr;
    }
}
