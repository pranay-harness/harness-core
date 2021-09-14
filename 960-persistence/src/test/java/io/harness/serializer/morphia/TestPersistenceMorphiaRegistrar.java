/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.serializer.morphia;

import io.harness.dataretention.AccountDataRetentionTestEntity;
import io.harness.iterator.TestCronIterableEntity;
import io.harness.iterator.TestFibonacciIterableEntity;
import io.harness.iterator.TestIrregularIterableEntity;
import io.harness.iterator.TestIterableEntity;
import io.harness.iterator.TestRegularIterableEntity;
import io.harness.mongo.TestIndexEntity;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;
import io.harness.persistence.MorphiaClass;
import io.harness.persistence.TestHolderEntity;
import io.harness.persistence.converters.DurationTestEntity;
import io.harness.persistence.converters.ObjectArrayTestEntity;
import io.harness.queue.TestInternalEntity;
import io.harness.queue.TestNoTopicQueuableObject;
import io.harness.queue.TestTopicQueuableObject;

import java.util.Set;

public class TestPersistenceMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(AccountDataRetentionTestEntity.class);
    set.add(DurationTestEntity.class);
    set.add(ObjectArrayTestEntity.class);
    set.add(TestCronIterableEntity.class);
    set.add(TestFibonacciIterableEntity.class);
    set.add(TestHolderEntity.class);
    set.add(TestIndexEntity.class);
    set.add(TestInternalEntity.class);
    set.add(TestIrregularIterableEntity.class);
    set.add(TestIterableEntity.class);
    set.add(TestNoTopicQueuableObject.class);
    set.add(TestRegularIterableEntity.class);
    set.add(TestTopicQueuableObject.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    // from commons
    h.put("persistence.MorphiaOldClass", MorphiaClass.class);
  }
}
