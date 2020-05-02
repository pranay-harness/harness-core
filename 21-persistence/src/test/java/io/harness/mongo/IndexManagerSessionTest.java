package io.harness.mongo;

import static io.harness.mongo.IndexManager.Mode.AUTO;
import static io.harness.mongo.IndexManagerCollectionSession.createCollectionSession;
import static io.harness.rule.OwnerRule.GEORGE;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.inject.Inject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import io.harness.PersistenceTest;
import io.harness.category.element.UnitTests;
import io.harness.mongo.IndexManager.IndexCreator;
import io.harness.mongo.IndexManager.IndexCreator.IndexCreatorBuilder;
import io.harness.mongo.IndexManagerSession.Accesses;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import io.harness.rule.RealMongo;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.mapping.MappedClass;

import java.time.Duration;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

public class IndexManagerSessionTest extends PersistenceTest {
  @Inject HPersistence persistence;

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  @RealMongo
  public void testMongoBehaviorCreateIndexWithExistingMatchingFields() {
    IndexManagerSession session = new IndexManagerSession(AUTO);
    DBCollection collection = persistence.getCollection(TestIndexEntity.class);

    IndexCreator original = buildIndexCreator(collection, "foo", 1).build();
    session.create(original);

    IndexCreator indexCreator = buildIndexCreator(collection, "foo2", 1).build();
    session.create(indexCreator);

    DBObject dbObject = createCollectionSession(collection).findIndexByFields(indexCreator);
    assertThat(dbObject.get("name")).isEqualTo("foo");

    assertThat(session.rebuildIndex(createCollectionSession(collection), indexCreator, ofSeconds(0))).isTrue();

    DBObject dbObject2 = createCollectionSession(collection).findIndexByFields(indexCreator);
    assertThat(dbObject2.get("name")).isEqualTo("foo2");
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  @RealMongo
  public void testMongoBehaviorCreateIndexWithExistingNonMatchingFields() {
    IndexManagerSession session = new IndexManagerSession(AUTO);
    DBCollection collection = persistence.getCollection(TestIndexEntity.class);

    IndexCreator original = buildIndexCreator(collection, "foo", 1).build();
    session.create(original);

    IndexCreator indexCreator = buildIndexCreator(collection, "foo", -1).build();
    assertThatThrownBy(() -> session.create(indexCreator))
        .hasMessageContaining("has the same name as the requested index");

    DBObject dbObject = createCollectionSession(collection).findIndexByFields(original);
    assertThat(dbObject.get("name")).isEqualTo("foo");

    assertThat(session.rebuildIndex(createCollectionSession(collection), indexCreator, ofSeconds(0))).isTrue();
    session.create(indexCreator);
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  @RealMongo
  public void testMongoRebuiltIndexInTime() {
    IndexManagerSession session = new IndexManagerSession(AUTO);
    DBCollection collection = persistence.getCollection(TestIndexEntity.class);

    IndexCreator original = buildIndexCreator(collection, "foo", 1).build();
    session.create(original);

    assertThat(collection.getIndexInfo()).hasSize(2);

    IndexCreator indexCreator = buildIndexCreator(collection, "foo2", 1).build();

    assertThat(session.rebuildIndex(createCollectionSession(collection), indexCreator, ofSeconds(120))).isFalse();

    // The rebuild did not drop the original but created the temporary one
    assertThat(collection.getIndexInfo()).hasSize(3);

    DBObject dbObject2 = createCollectionSession(collection).findIndexByFields(indexCreator);
    assertThat(dbObject2.get("name")).isEqualTo("foo");

    assertThat(session.rebuildIndex(createCollectionSession(collection), indexCreator, ofSeconds(0))).isTrue();

    // The original is dropped and regenerated
    assertThat(collection.getIndexInfo()).hasSize(3);

    DBObject dbObject3 = createCollectionSession(collection).findIndexByFields(indexCreator);
    assertThat(dbObject3.get("name")).isEqualTo("foo2");
  }

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
  public void testIndexCreators() {
    Morphia morphia = new Morphia();
    morphia.map(TestIndexEntity.class);

    Collection<MappedClass> mappedClasses = morphia.getMapper().getMappedClasses();

    MappedClass mappedClass = mappedClasses.iterator().next();

    DBCollection collection = persistence.getCollection(mappedClass.getClazz());

    Map<String, IndexCreator> creators = IndexManager.indexCreators(mappedClass, collection);

    assertThat(creators).hasSize(4);
    assertThat(creators.get("sparse_index").getOptions().get(IndexManagerSession.SPARSE)).isEqualTo(Boolean.TRUE);
    assertThat(creators.get("sparseTest_1").getOptions().get(IndexManagerSession.SPARSE)).isEqualTo(Boolean.TRUE);

    IndexManagerSession session = new IndexManagerSession(AUTO);
    assertThat(session.createNewIndexes(createCollectionSession(collection), creators)).isEqualTo(creators.size());
    Date afterCreatingIndexes = new Date();

    Map<String, Accesses> accesses = IndexManagerSession.fetchIndexAccesses(collection);
    Date tooNew = new Date(System.currentTimeMillis() - Duration.ofDays(1).toMillis());
    assertThat(createCollectionSession(collection).isOkToDropIndexes(IndexManagerSession.tooNew(), accesses)).isFalse();
    assertThat(createCollectionSession(collection).isOkToDropIndexes(afterCreatingIndexes, accesses)).isTrue();
  }
}