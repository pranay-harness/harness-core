package io.harness.iterator;

import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAccess;

public interface PersistentIterable extends PersistentEntity, UuidAccess { Long obtainNextIteration(String fieldName); }
