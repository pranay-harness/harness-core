package io.harness.mongo;

import static io.harness.rule.OwnerRule.UNKNOWN;

import com.google.common.collect.ImmutableSet;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.iterator.TestCronIterableEntity;
import io.harness.iterator.TestIrregularIterableEntity;
import io.harness.iterator.TestIterableEntity;
import io.harness.iterator.TestRegularIterableEntity;
import io.harness.morphia.MorphiaModule;
import io.harness.persistence.TestHolderEntity;
import io.harness.queue.TestInternalEntity;
import io.harness.queue.TestUnversionedQueuableObject;
import io.harness.queue.TestVersionedQueuableObject;
import io.harness.rule.OwnerRule.Owner;
import io.harness.serializer.morphia.PersistenceMorphiaRegistrar;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class PersistenceMorphiaClassesTest extends CategoryTest {
  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void testPersistenceModule() {
    new PersistenceMorphiaRegistrar().testClassesModule();
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void testPersistenceSearchAndList() {
    new MorphiaModule().testAutomaticSearch(ImmutableSet.<Class>builder()
                                                .add(TestCronIterableEntity.class)
                                                .add(TestHolderEntity.class)
                                                .add(TestInternalEntity.class)
                                                .add(TestIrregularIterableEntity.class)
                                                .add(TestIterableEntity.class)
                                                .add(TestVersionedQueuableObject.class)
                                                .add(TestUnversionedQueuableObject.class)
                                                .add(TestRegularIterableEntity.class)
                                                .build());
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void testPersistencImplementationClassesModule() {
    new PersistenceMorphiaRegistrar().testImplementationClassesModule();
  }
}