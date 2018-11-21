package io.harness.mongo;

import com.google.common.collect.ImmutableSet;

import io.harness.beans.Encryptable;
import io.harness.persistence.CreatedAtAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAccess;
import io.harness.persistence.UuidAware;
import io.harness.queue.Queuable;

import java.util.Set;

public class PersistenceMorphiaClasses {
  public static final Set<Class> classes = ImmutableSet.<Class>of(Queuable.class, Encryptable.class, UuidAware.class,
      CreatedAtAware.class, CreatedAtAccess.class, PersistentEntity.class, UuidAccess.class);
}
