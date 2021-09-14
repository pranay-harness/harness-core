/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness;

import static io.harness.rule.OwnerRule.RAGHU;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.cvng.VerificationApplication;
import io.harness.iterator.PersistentIterable;
import io.harness.iterator.PersistentRegularIterable;
import io.harness.persistence.PersistentEntity;
import io.harness.rule.Owner;
import io.harness.serializer.morphia.CVNextGenMorphiaRegister;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.reflections.Reflections;

public class CVNGMorphiaRegistrarTest extends CvNextGenTestBase {
  @Inject private CVNextGenMorphiaRegister cvNextGenMorphiaRegister;

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testMorphiaRegistrar() {
    Set<Class> excludedClasses = Sets.newHashSet(PersistentIterable.class, PersistentRegularIterable.class);
    Set<Class> registeredClasses = new HashSet<>();
    cvNextGenMorphiaRegister.registerClasses(registeredClasses);
    Reflections reflections = new Reflections(VerificationApplication.class.getPackage().getName());
    Set<Class<? extends PersistentEntity>> cvngEntityClasses = reflections.getSubTypesOf(PersistentEntity.class);
    cvngEntityClasses.removeAll(excludedClasses);
    cvngEntityClasses.removeAll(registeredClasses);
    assertThat(cvngEntityClasses.isEmpty())
        .withFailMessage("the following classes are not registered with morphia registrar %s", cvngEntityClasses)
        .isTrue();
  }
}
