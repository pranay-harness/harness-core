package io.harness.mongo;

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
import io.harness.mongo.IndexManager.Accesses;
import io.harness.mongo.IndexManager.IndexCreator;
import io.harness.mongo.IndexManager.IndexCreator.IndexCreatorBuilder;
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
import java.util.List;
import java.util.Map;

public class IndexManagerTest extends PersistenceTest {
  @Inject HPersistence persistence;

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  @RealMongo
  public void testMongoBehaviorCreateIndexWithExistingMatchingFields() {
    DBCollection collection = persistence.getCollection(TestIndexEntity.class);
    IndexCreator original = buildIndexCreator(collection, "foo", 1).build();
    original.create();

    IndexCreator indexCreator = buildIndexCreator(collection, "foo2", 1).build();
    indexCreator.create();

    DBObject dbObject = IndexManager.findIndexByFields(indexCreator);
    assertThat(dbObject.get("name")).isEqualTo("foo");

    IndexManager.rebuildIndex(indexCreator, ofSeconds(0));

    DBObject dbObject2 = IndexManager.findIndexByFields(indexCreator);
    assertThat(dbObject2.get("name")).isEqualTo("foo2");
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  @RealMongo
  public void testMongoBehaviorCreateIndexWithExistingNonMatchingFields() {
    DBCollection collection = persistence.getCollection(TestIndexEntity.class);
    IndexCreator original = buildIndexCreator(collection, "foo", 1).build();
    original.create();

    IndexCreator indexCreator = buildIndexCreator(collection, "foo", -1).build();
    assertThatThrownBy(() -> indexCreator.create()).hasMessageContaining("has the same name as the requested index");

    DBObject dbObject = IndexManager.findIndexByFields(original);
    assertThat(dbObject.get("name")).isEqualTo("foo");

    IndexManager.rebuildIndex(indexCreator, ofSeconds(0));
    indexCreator.create();
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  @RealMongo
  public void testMongoRebuiltIndexInTime() {
    DBCollection collection = persistence.getCollection(TestIndexEntity.class);
    IndexCreator original = buildIndexCreator(collection, "foo", 1).build();
    original.create();
    assertThat(collection.getIndexInfo()).hasSize(2);

    IndexCreator indexCreator = buildIndexCreator(collection, "foo2", 1).build();

    IndexManager.rebuildIndex(indexCreator, ofSeconds(120));

    // The rebuild did not drop the original but created the temporary one
    assertThat(collection.getIndexInfo()).hasSize(3);

    DBObject dbObject2 = IndexManager.findIndexByFields(indexCreator);
    assertThat(dbObject2.get("name")).isEqualTo("foo");

    IndexManager.rebuildIndex(indexCreator, ofSeconds(0));

    // The original is dropped and regenerated
    assertThat(collection.getIndexInfo()).hasSize(3);

    DBObject dbObject3 = IndexManager.findIndexByFields(indexCreator);
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

    DBCollection collection = getDatastore().getCollection(mappedClass.getClazz());

    Map<String, IndexCreator> creators = IndexManager.indexCreators(mappedClass, collection);

    assertThat(creators).hasSize(2);
    assertThat(IndexManager.createNewIndexes(creators)).isEqualTo(creators.size());
    Date afterCreatingIndexes = new Date();

    Map<String, Accesses> accesses = IndexManager.fetchIndexAccesses(collection);
    List<DBObject> indexInfo = collection.getIndexInfo();

    Date tooNew = new Date(System.currentTimeMillis() - Duration.ofDays(1).toMillis());
    assertThat(IndexManager.isOkToDropIndexes(IndexManager.tooNew(), accesses, indexInfo)).isFalse();
    assertThat(IndexManager.isOkToDropIndexes(afterCreatingIndexes, accesses, indexInfo)).isTrue();
  }
}