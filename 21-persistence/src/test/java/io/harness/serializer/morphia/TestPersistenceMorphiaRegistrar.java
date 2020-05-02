package io.harness.serializer.morphia;

import io.harness.globalcontex.AuditGlobalContextData;
import io.harness.iterator.TestCronIterableEntity;
import io.harness.iterator.TestIrregularIterableEntity;
import io.harness.iterator.TestIterableEntity;
import io.harness.iterator.TestRegularIterableEntity;
import io.harness.limits.impl.model.RateLimit;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.persistence.MorphiaClass;
import io.harness.persistence.TestHolderEntity;
import io.harness.queue.TestInternalEntity;
import io.harness.queue.TestNoTopicQueuableObject;
import io.harness.queue.TestTopicQueuableObject;

import java.util.Map;
import java.util.Set;

public class TestPersistenceMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(TestCronIterableEntity.class);
    set.add(TestHolderEntity.class);
    set.add(TestInternalEntity.class);
    set.add(TestIrregularIterableEntity.class);
    set.add(TestIterableEntity.class);
    set.add(TestTopicQueuableObject.class);
    set.add(TestNoTopicQueuableObject.class);
    set.add(TestRegularIterableEntity.class);
  }

  @Override
  public void registerImplementationClasses(Map<String, Class> map) {
    final HelperPut h = (name, clazz) -> {
      map.put(PKG_HARNESS + name, clazz);
    };

    // from commons
    h.put("persistence.MorphiaOldClass", MorphiaClass.class);
    h.put("limits.impl.model.RateLimit", RateLimit.class);

    // from api-service
    h.put("globalcontex.AuditGlobalContextData", AuditGlobalContextData.class);
  }
}
