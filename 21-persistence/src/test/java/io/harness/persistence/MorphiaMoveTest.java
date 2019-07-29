package io.harness.persistence;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;

import io.harness.PersistenceTest;
import io.harness.category.element.UnitTests;
import io.harness.mongo.MorphiaMove;
import lombok.Builder;
import lombok.Value;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

interface MorphiaInterface {}

@Builder
class HackMorphiaClass implements MorphiaInterface {
  private String test;
  private String className;
}

@Value
@Builder
@Entity(value = "!!!testHolder", noClassnameStored = true)
class HolderEntity implements PersistentEntity {
  @Id private String uuid;
  MorphiaInterface morphiaObj;
}

public class MorphiaMoveTest extends PersistenceTest {
  @Inject private HPersistence persistence;

  @Test
  @Category(UnitTests.class)
  public void shouldReadOldClass() {
    HolderEntity entity =
        HolderEntity.builder()
            .uuid(generateUuid())
            .morphiaObj(
                HackMorphiaClass.builder().test("test").className("io.harness.persistence.MorphiaOldClass").build())
            .build();
    String id = persistence.save(entity);
    assertThat(id).isNotNull();

    final HolderEntity holderEntity = persistence.get(HolderEntity.class, id);
    assertThat(((MorphiaClass) holderEntity.getMorphiaObj()).getTest()).isEqualTo("test");
  }

  @Test
  @Category(UnitTests.class)
  public void shouldReadFutureClass() {
    persistence.save(MorphiaMove.builder()
                         .target("io.harness.persistence.MorphiaFeatureClass")
                         .sources(ImmutableSet.of("io.harness.persistence.MorphiaClass"))
                         .build());

    HolderEntity entity =
        HolderEntity.builder()
            .uuid(generateUuid())
            .morphiaObj(
                HackMorphiaClass.builder().test("test").className("io.harness.persistence.MorphiaFeatureClass").build())
            .build();
    String id = persistence.save(entity);
    assertThat(id).isNotNull();

    final HolderEntity holderEntity = persistence.get(HolderEntity.class, id);
    assertThat(((MorphiaClass) holderEntity.getMorphiaObj()).getTest()).isEqualTo("test");
  }
}
