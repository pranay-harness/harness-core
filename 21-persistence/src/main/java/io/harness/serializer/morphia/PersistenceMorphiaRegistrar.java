package io.harness.serializer.morphia;

import io.harness.beans.Encryptable;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.command.CommandExecutionResult;
import io.harness.globalcontex.AuditGlobalContextData;
import io.harness.globalcontex.PurgeGlobalContextData;
import io.harness.iterator.PersistentCronIterable;
import io.harness.iterator.PersistentIrregularIterable;
import io.harness.iterator.PersistentIterable;
import io.harness.iterator.PersistentRegularIterable;
import io.harness.limits.impl.model.RateLimit;
import io.harness.limits.impl.model.StaticLimit;
import io.harness.migration.MigrationJobInstance;
import io.harness.mongo.MorphiaMove;
import io.harness.mongo.MorphiaRegistrar;
import io.harness.persistence.CreatedAtAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.CreatedByAccess;
import io.harness.persistence.CreatedByAware;
import io.harness.persistence.GoogleDataStoreAware;
import io.harness.persistence.NameAccess;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAccess;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UpdatedByAccess;
import io.harness.persistence.UpdatedByAware;
import io.harness.persistence.UuidAccess;
import io.harness.persistence.UuidAware;
import io.harness.queue.Queuable;
import io.harness.security.SimpleEncryption;
import io.harness.security.encryption.EncryptedRecord;
import io.harness.security.encryption.EncryptionConfig;
import io.harness.tags.TagAware;

import java.util.Map;
import java.util.Set;

public class PersistenceMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void register(Set<Class> set) {
    // from commons
    set.add(EncryptionConfig.class);

    // from api-service
    set.add(EncryptedRecord.class);

    // from delegate-task-beans
    set.add(ExecutionCapabilityDemander.class);

    registerMyClasses(set);
  }

  public void registerMyClasses(Set<Class> set) {
    set.add(CreatedAtAccess.class);
    set.add(CreatedAtAware.class);
    set.add(CreatedByAccess.class);
    set.add(CreatedByAware.class);
    set.add(Encryptable.class);
    set.add(GoogleDataStoreAware.class);
    set.add(MigrationJobInstance.class);
    set.add(MorphiaMove.class);
    set.add(NameAccess.class);
    set.add(PersistentCronIterable.class);
    set.add(PersistentEntity.class);
    set.add(PersistentIrregularIterable.class);
    set.add(PersistentIterable.class);
    set.add(PersistentRegularIterable.class);
    set.add(Queuable.class);
    set.add(TagAware.class);
    set.add(UpdatedAtAccess.class);
    set.add(UpdatedAtAware.class);
    set.add(UpdatedByAccess.class);
    set.add(UpdatedByAware.class);
    set.add(UuidAccess.class);
    set.add(UuidAware.class);
  }

  @Override
  public void register(Map<String, Class> map) {
    final HelperPut h = (name, clazz) -> {
      map.put(pkgHarness + name, clazz);
    };

    // from commons
    h.put("limits.impl.model.StaticLimit", StaticLimit.class);
    h.put("limits.impl.model.RateLimit", RateLimit.class);
    h.put("security.SimpleEncryption", SimpleEncryption.class);

    // from api-service
    h.put("globalcontex.AuditGlobalContextData", AuditGlobalContextData.class);
    h.put("globalcontex.PurgeGlobalContextData", PurgeGlobalContextData.class);

    // from delegate-task-beans
    h.put("delegate.command.CommandExecutionResult", CommandExecutionResult.class);
  }
}
