package io.harness.persistence;

public interface PersistentIterable extends PersistentEntity, UuidAccess { Long obtainNextIteration(String fieldName); }
