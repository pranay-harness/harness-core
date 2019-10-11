package io.harness.mongo;

import com.google.common.collect.ImmutableSet;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.iterator.TestCronIterableEntity;
import io.harness.iterator.TestIrregularIterableEntity;
import io.harness.iterator.TestRegularIterableEntity;
import io.harness.morphia.MorphiaModule;
import io.harness.persistence.TestHolderEntity;
import io.harness.queue.TestInternalEntity;
import io.harness.queue.TestQueuableObject;
import io.harness.serializer.morphia.PersistenceMorphiaRegistrar;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class PersistenceMorphiaClassesTest extends CategoryTest {
  @Test
  @Category(UnitTests.class)
  public void testPersistenceModule() {
    new PersistenceMorphiaRegistrar().testClassesModule();
  }

  @Test
  @Category(UnitTests.class)
  public void testPersistenceSearchAndList() {
    new MorphiaModule().testAutomaticSearch(ImmutableSet.<Class>builder()
                                                .add(TestCronIterableEntity.class)
                                                .add(TestHolderEntity.class)
                                                .add(TestInternalEntity.class)
                                                .add(TestIrregularIterableEntity.class)
                                                .add(TestQueuableObject.class)
                                                .add(TestRegularIterableEntity.class)
                                                .build());
  }

  @Test
  @Category(UnitTests.class)
  public void testPersistencImplementationClassesModule() {
    new PersistenceMorphiaRegistrar().testImplementationClassesModule();
  }
}