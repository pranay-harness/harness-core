package io.harness.persistence;

public interface PersistentIterable extends PersistentEntity, UuidAccess { Long getNextIteration(String fieldName); }
