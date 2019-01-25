package io.harness.iterator;

import io.harness.persistence.PersistentIterable;

public interface PersistenceIterator<T extends PersistentIterable> { void process(); }
