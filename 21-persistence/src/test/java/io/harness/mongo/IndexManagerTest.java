package io.harness.mongo;

import static io.harness.rule.OwnerRule.GEORGE;
import static org.assertj.core.api.Assertions.assertThat;

import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import io.harness.PersistenceTest;
import io.harness.category.element.UnitTests;
import io.harness.mongo.IndexManager.Accesses;
import io.harness.mongo.IndexManager.IndexCreator;
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
  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  @RealMongo
  public void testIndexCreators() {
    final Morphia morphia = new Morphia();
    morphia.map(TestIndexEntity.class);

    final Collection<MappedClass> mappedClasses = morphia.getMapper().getMappedClasses();

    final MappedClass mappedClass = mappedClasses.iterator().next();

    final DBCollection collection = getDatastore().getCollection(mappedClass.getClazz());

    final Map<String, IndexCreator> creators = IndexManager.indexCreators(mappedClass, collection);

    assertThat(creators).hasSize(3);
    assertThat(IndexManager.createNewIndexes(creators)).isEqualTo(creators.size());
    final Date afterCreatingIndexes = new Date();

    final Map<String, Accesses> accesses = IndexManager.fetchIndexAccesses(collection);
    final List<DBObject> indexInfo = collection.getIndexInfo();

    final Date tooNew = new Date(System.currentTimeMillis() - Duration.ofDays(1).toMillis());
    assertThat(IndexManager.isOkToDropIndexes(IndexManager.tooNew(), accesses, indexInfo)).isFalse();
    assertThat(IndexManager.isOkToDropIndexes(afterCreatingIndexes, accesses, indexInfo)).isTrue();
  }
}