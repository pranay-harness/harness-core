package io.harness.persistence;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.PersistenceTest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.assertj.core.util.Lists;
import org.junit.Test;
import org.mongodb.morphia.annotations.Id;

import java.util.List;
import java.util.stream.IntStream;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
class TestEntity extends PersistentEntity {
  @Id private String uuid;
  private String test;
}

public class HPersistenceTest extends PersistenceTest {
  @Inject private HPersistence persistence;

  @Test
  public void shouldSave() {
    TestEntity entity = TestEntity.builder().uuid(generateUuid()).test("foo").build();
    String id = persistence.save(entity);
    assertThat(id).isNotNull();
  }

  @Test
  public void shouldSaveList() {
    List<TestEntity> list = Lists.newArrayList();
    IntStream.range(0, 5).forEach(i -> {
      TestEntity entity = TestEntity.builder().uuid(generateUuid()).test("foo" + i).build();
      list.add(entity);
    });
    List<String> ids = persistence.save(list);
    assertThat(ids).isNotNull().hasSize(list.size()).doesNotContainNull();
  }

  @Test
  public void shouldSaveIgnoringDuplicateKeysList() {
    List<TestEntity> list = Lists.newArrayList();
    IntStream.range(0, 5).forEach(i -> {
      TestEntity entity = TestEntity.builder().uuid(generateUuid()).test("shouldSaveIgnoringDuplicateKeysList").build();
      list.add(entity);
    });

    persistence.save(list.get(0));
    persistence.saveIgnoringDuplicateKeys(list);

    final List<TestEntity> testEntities =
        persistence.createQuery(TestEntity.class).filter("test", "shouldSaveIgnoringDuplicateKeysList").asList();

    assertThat(testEntities).hasSize(5);
  }
}
