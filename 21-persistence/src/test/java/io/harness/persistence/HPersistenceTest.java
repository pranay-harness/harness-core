package io.harness.persistence;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.rule.OwnerRule.GEORGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import com.google.inject.Inject;

import com.mongodb.DuplicateKeyException;
import io.harness.PersistenceTest;
import io.harness.category.element.UnitTests;
import io.harness.persistence.TestEntity.TestEntityKeys;
import io.harness.rule.Owner;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.assertj.core.util.Lists;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.query.Query;

import java.util.List;
import java.util.stream.IntStream;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants(innerTypeName = "TestEntityKeys")
class TestEntity implements PersistentEntity, UuidAccess {
  @Id private String uuid;
  private String test;
}

public class HPersistenceTest extends PersistenceTest {
  @Inject private HPersistence persistence;

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldSave() {
    TestEntity entity = TestEntity.builder().uuid(generateUuid()).test("foo").build();
    String id = persistence.save(entity);
    assertThat(id).isNotNull();
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
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
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldSaveIgnoringDuplicateKeysList() {
    List<TestEntity> list = Lists.newArrayList();
    IntStream.range(0, 5).forEach(i -> {
      TestEntity entity = TestEntity.builder().uuid(generateUuid()).test("shouldSaveIgnoringDuplicateKeysList").build();
      list.add(entity);
    });

    persistence.save(list.get(0));
    persistence.saveIgnoringDuplicateKeys(list);

    final List<TestEntity> testEntities = persistence.createQuery(TestEntity.class, excludeAuthority)
                                              .filter(TestEntityKeys.test, "shouldSaveIgnoringDuplicateKeysList")
                                              .asList();

    assertThat(testEntities).hasSize(5);
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldInsert() {
    TestEntity entity = TestEntity.builder().uuid(generateUuid()).test("foo").build();
    String id = persistence.insert(entity);
    assertThat(id).isNotNull();
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldNotInsertTwice() {
    TestEntity entity = TestEntity.builder().uuid(generateUuid()).test("foo").build();
    persistence.insert(entity);

    assertThatThrownBy(() -> persistence.insert(entity)).isInstanceOf(DuplicateKeyException.class);
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldInsertIgnoringDuplicateKeys() {
    TestEntity entity = TestEntity.builder().uuid(generateUuid()).test("foo").build();
    persistence.insert(entity);

    String id = persistence.insertIgnoringDuplicateKeys(entity);
    assertThat(id).isNotNull();
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldGet() {
    TestEntity entity = TestEntity.builder().uuid(generateUuid()).test("shouldGet").build();
    persistence.save(entity);

    final TestEntity obtainedEntity = persistence.get(TestEntity.class, entity.getUuid());
    assertThat(obtainedEntity.getTest()).isEqualTo("shouldGet");
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldDeleteUuid() {
    TestEntity entity = TestEntity.builder().uuid(generateUuid()).test("shouldDeleteUuid").build();

    persistence.save(entity);

    final Query<TestEntity> query =
        persistence.createQuery(TestEntity.class, excludeAuthority).filter(TestEntityKeys.test, "shouldDeleteUuid");

    List<TestEntity> testEntities = query.asList();
    assertThat(testEntities).hasSize(1);

    persistence.delete(TestEntity.class, entity.getUuid());

    testEntities = query.asList();
    assertThat(testEntities).hasSize(0);
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldDeleteQuery() {
    TestEntity entity = TestEntity.builder().uuid(generateUuid()).test("shouldDeleteQuery").build();

    persistence.save(entity);

    final Query<TestEntity> query =
        persistence.createQuery(TestEntity.class, excludeAuthority).filter(TestEntityKeys.test, "shouldDeleteQuery");

    List<TestEntity> testEntities = query.asList();
    assertThat(testEntities).hasSize(1);

    persistence.delete(query);

    testEntities = query.asList();
    assertThat(testEntities).hasSize(0);
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldDeleteEntity() {
    TestEntity entity = TestEntity.builder().uuid(generateUuid()).test("shouldDeleteEntity").build();

    persistence.save(entity);

    final Query<TestEntity> query =
        persistence.createQuery(TestEntity.class, excludeAuthority).filter(TestEntityKeys.test, "shouldDeleteEntity");

    List<TestEntity> testEntities = query.asList();
    assertThat(testEntities).hasSize(1);

    persistence.delete(entity);

    testEntities = query.asList();
    assertThat(testEntities).hasSize(0);
  }
}
