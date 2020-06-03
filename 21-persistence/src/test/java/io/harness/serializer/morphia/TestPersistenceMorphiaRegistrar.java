package io.harness.serializer.morphia;

import io.harness.iterator.TestCronIterableEntity;
import io.harness.iterator.TestIrregularIterableEntity;
import io.harness.iterator.TestIterableEntity;
import io.harness.iterator.TestRegularIterableEntity;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.persistence.MorphiaClass;
import io.harness.persistence.TestHolderEntity;
import io.harness.queue.TestInternalEntity;
import io.harness.queue.TestNoTopicQueuableObject;
import io.harness.queue.TestTopicQueuableObject;

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
  public void registerImplementationClasses(HelperPut h, HelperPut w) {
    // from commons
    h.put("persistence.MorphiaOldClass", MorphiaClass.class);
  }
}
