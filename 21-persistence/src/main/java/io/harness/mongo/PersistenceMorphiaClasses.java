package io.harness.mongo;

import com.google.common.collect.ImmutableSet;

import io.harness.persistence.CreatedAtAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAccess;
import io.harness.persistence.UuidAware;

import java.util.Set;

public class PersistenceMorphiaClasses {
  public static final Set<Class> classes = ImmutableSet.<Class>of(
      UuidAware.class, CreatedAtAware.class, CreatedAtAccess.class, PersistentEntity.class, UuidAccess.class);
}
