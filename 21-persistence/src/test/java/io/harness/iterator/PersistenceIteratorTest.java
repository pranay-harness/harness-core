package io.harness.iterator;

import static io.harness.rule.OwnerRule.GEORGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;

import com.google.inject.Inject;

import io.harness.PersistenceTest;
import io.harness.category.element.UnitTests;
import io.harness.iterator.TestIterableEntity.TestIterableEntityKeys;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mongodb.morphia.query.Query;

@Slf4j
public class PersistenceIteratorTest extends PersistenceTest {
  @Inject private HPersistence persistence;

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testCreateQueryWithNoFilter() {
    final MongoPersistenceIterator<TestIterableEntity> iterator = MongoPersistenceIterator.<TestIterableEntity>builder()
                                                                      .clazz(TestIterableEntity.class)
                                                                      .fieldName(TestIterableEntityKeys.nextIterations)
                                                                      .filterExpander(null)
                                                                      .build();

    on(iterator).set("persistence", persistence);
    final Query<TestIterableEntity> query = iterator.createQuery();

    assertThat(query.toString().replace(" ", "")).isEqualTo("{query:{}}");
    assertThat(iterator.createQuery(5).toString().replace(" ", ""))
        .isEqualTo("{query:{\"$or\":"
            + "[{\"nextIterations\":{\"$lt\":5}},"
            + "{\"nextIterations\":{\"$exists\":false}}]}}");
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testCreateQueryWithSimpleFilter() {
    final MongoPersistenceIterator<TestIterableEntity> iterator =
        MongoPersistenceIterator.<TestIterableEntity>builder()
            .clazz(TestIterableEntity.class)
            .fieldName(TestIterableEntityKeys.nextIterations)
            .filterExpander(query -> query.filter(TestIterableEntityKeys.name, "foo"))
            .build();

    on(iterator).set("persistence", persistence);
    final Query<TestIterableEntity> query = iterator.createQuery();

    assertThat(query.toString().replace(" ", "")).isEqualTo("{query:{\"name\":\"foo\"}}");
    assertThat(iterator.createQuery(5).toString().replace(" ", ""))
        .isEqualTo("{query:{\"name\":\"foo\",\"$or\":"
            + "[{\"nextIterations\":{\"$lt\":5}},"
            + "{\"nextIterations\":{\"$exists\":false}}]}}");
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testCreateQueryWithAndFilter() {
    final MongoPersistenceIterator<TestIterableEntity> iterator =
        MongoPersistenceIterator.<TestIterableEntity>builder()
            .clazz(TestIterableEntity.class)
            .fieldName(TestIterableEntityKeys.nextIterations)
            .filterExpander(query
                -> query.and(query.criteria(TestIterableEntityKeys.name).exists(),
                    query.criteria(TestIterableEntityKeys.name).equal("foo")))
            .build();

    on(iterator).set("persistence", persistence);
    final Query<TestIterableEntity> query = iterator.createQuery();

    assertThat(query.toString().replace(" ", ""))
        .isEqualTo("{query:{\"$and\":[{\"name\":{\"$exists\":true}},{\"name\":\"foo\"}]}}");
    assertThat(iterator.createQuery(5).toString().replace(" ", ""))
        .isEqualTo("{query:{\"$and\":[{\"$and\":[{\"name\":{\"$exists\":true}},{\"name\":\"foo\"}]},"
            + "{\"$or\":[{\"nextIterations\":{\"$lt\":5}},"
            + "{\"nextIterations\":{\"$exists\":false}}]}]}}");
  }
}