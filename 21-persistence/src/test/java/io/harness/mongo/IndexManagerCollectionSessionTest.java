package io.harness.mongo;

import static io.harness.mongo.IndexManager.Mode.AUTO;
import static io.harness.mongo.IndexManagerCollectionSession.createCollectionSession;
import static io.harness.rule.OwnerRule.GEORGE;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import io.harness.PersistenceTest;
import io.harness.category.element.UnitTests;
import io.harness.mongo.IndexManager.IndexCreator;
import io.harness.mongo.IndexManager.IndexCreator.IndexCreatorBuilder;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import io.harness.rule.RealMongo;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.List;
import java.util.Set;

public class IndexManagerCollectionSessionTest extends PersistenceTest {
  @Inject HPersistence persistence;

  private IndexCreatorBuilder buildIndexCreator(DBCollection collection, String name, int i) {
    return IndexCreator.builder()
        .collection(collection)
        .keys(new BasicDBObject().append("a", i).append("b", 1))
        .options(new BasicDBObject().append("name", name));
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  @RealMongo
  public void testFindIndexByFields() {
    IndexManagerSession session = new IndexManagerSession(AUTO);
    DBCollection collection = persistence.getCollection(TestIndexEntity.class);

    IndexCreator indexCreator = buildIndexCreator(collection, "foo", 1).build();
    assertThat(createCollectionSession(collection).findIndexByFields(indexCreator)).isNull();

    session.create(indexCreator);

    DBObject dbObject = createCollectionSession(collection).findIndexByFields(indexCreator);
    assertThat(dbObject.get("name")).isEqualTo("foo");
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  @RealMongo
  public void testIsRebuiltNeeded() {
    IndexManagerSession session = new IndexManagerSession(AUTO);
    DBCollection collection = persistence.getCollection(TestIndexEntity.class);

    IndexCreator indexCreator = buildIndexCreator(collection, "foo", 1).build();
    assertThat(createCollectionSession(collection).isRebuildNeeded(indexCreator)).isFalse();

    session.create(indexCreator);

    assertThat(createCollectionSession(collection).isRebuildNeeded(indexCreator)).isFalse();

    IndexCreator differentCreator = buildIndexCreator(collection, "foo2", 1).build();
    assertThat(createCollectionSession(collection).isRebuildNeeded(differentCreator)).isTrue();
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  @RealMongo
  public void testIsCreateNeeded() {
    IndexManagerSession session = new IndexManagerSession(AUTO);
    DBCollection collection = persistence.getCollection(TestIndexEntity.class);

    IndexCreator indexCreator = buildIndexCreator(collection, "foo", 1).build();
    assertThat(createCollectionSession(collection).isCreateNeeded(indexCreator)).isTrue();

    session.create(indexCreator);

    assertThat(createCollectionSession(collection).isCreateNeeded(indexCreator)).isFalse();
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  @RealMongo
  public void obsoleteIndexes() {
    IndexManagerSession session = new IndexManagerSession(AUTO);
    DBCollection collection = persistence.getCollection(TestIndexEntity.class);

    Set<String> names = ImmutableSet.<String>builder().add("foo2").build();

    session.create(buildIndexCreator(collection, "foo1", 1).build());
    session.create(buildIndexCreator(collection, "foo2", 1).build());

    List<String> obsoleteIndexes = createCollectionSession(collection).obsoleteIndexes(names);

    assertThat(obsoleteIndexes).containsExactly("foo1");
  }
}