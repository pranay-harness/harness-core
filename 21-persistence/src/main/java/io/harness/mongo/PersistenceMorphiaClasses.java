package io.harness.mongo;

import com.google.common.collect.ImmutableSet;

import io.harness.beans.Encryptable;
import io.harness.persistence.CreatedAtAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.CreatedByAccess;
import io.harness.persistence.CreatedByAware;
import io.harness.persistence.GoogleDataStoreAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.PersistentIterable;
import io.harness.persistence.UpdatedAtAccess;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UpdatedByAccess;
import io.harness.persistence.UpdatedByAware;
import io.harness.persistence.UuidAccess;
import io.harness.persistence.UuidAware;
import io.harness.queue.Queuable;

import java.util.Set;

public class PersistenceMorphiaClasses {
  public static final Set<Class> classes = ImmutableSet.<Class>of(Queuable.class, Encryptable.class, UuidAware.class,
      UuidAccess.class, CreatedAtAware.class, CreatedAtAccess.class, CreatedByAware.class, CreatedByAccess.class,
      UpdatedAtAware.class, UpdatedAtAccess.class, UpdatedByAware.class, UpdatedByAccess.class, PersistentEntity.class,
      GoogleDataStoreAware.class, PersistentIterable.class);
}
