package io.harness.persistence;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.GEORGE;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;

import io.harness.PersistenceTest;
import io.harness.category.element.UnitTests;
import io.harness.mongo.MorphiaMove;
import io.harness.rule.Owner;
import lombok.Builder;
import org.junit.Test;
import org.junit.experimental.categories.Category;

interface MorphiaInterface {}

@Builder
class HackMorphiaClass implements MorphiaInterface {
  private String test;
  private String className;
}

public class MorphiaMoveTest extends PersistenceTest {
  @Inject private HPersistence persistence;

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  @SuppressWarnings("PMD")
  public void shouldCacheMissingClass() {
    TestHolderEntity entity =
        TestHolderEntity.builder()
            .uuid(generateUuid())
            .morphiaObj(
                HackMorphiaClass.builder().test("test").className("io.harness.persistence.MorphiaMissingClass").build())
            .build();
    String id = persistence.save(entity);
    assertThat(id).isNotNull();

    try {
      persistence.get(TestHolderEntity.class, id);
    } catch (Throwable ignore) {
      // do nothing
    }
    try {
      persistence.get(TestHolderEntity.class, id);
    } catch (Throwable ignore) {
      // do nothing
    }
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldReadOldClass() {
    TestHolderEntity entity =
        TestHolderEntity.builder()
            .uuid(generateUuid())
            .morphiaObj(
                HackMorphiaClass.builder().test("test").className("io.harness.persistence.MorphiaOldClass").build())
            .build();
    String id = persistence.save(entity);
    assertThat(id).isNotNull();

    final TestHolderEntity holderEntity = persistence.get(TestHolderEntity.class, id);
    assertThat(((MorphiaClass) holderEntity.getMorphiaObj()).getTest()).isEqualTo("test");
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldReadFutureClass() {
    persistence.save(MorphiaMove.builder()
                         .target("io.harness.persistence.MorphiaFeatureClass")
                         .sources(ImmutableSet.of("io.harness.persistence.MorphiaClass"))
                         .build());

    TestHolderEntity entity =
        TestHolderEntity.builder()
            .uuid(generateUuid())
            .morphiaObj(
                HackMorphiaClass.builder().test("test").className("io.harness.persistence.MorphiaFeatureClass").build())
            .build();
    String id = persistence.save(entity);
    assertThat(id).isNotNull();

    final TestHolderEntity holderEntity = persistence.get(TestHolderEntity.class, id);
    assertThat(((MorphiaClass) holderEntity.getMorphiaObj()).getTest()).isEqualTo("test");
  }
}
