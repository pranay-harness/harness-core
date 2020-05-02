package io.harness.mongo;

import static io.harness.mongo.IndexManager.Mode.AUTO;
import static io.harness.mongo.IndexManager.Mode.INSPECT;
import static io.harness.rule.OwnerRule.GEORGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.inject.Inject;

import com.mongodb.BasicDBObject;
import io.harness.PersistenceTest;
import io.harness.category.element.UnitTests;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import io.harness.testlib.RealMongo;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mongodb.morphia.Morphia;

public class IndexManagerTest extends PersistenceTest {
  @Inject HPersistence persistence;

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  @RealMongo
  public void testMongoBehaviorCreateIndexWithExistingMatchingFields() {
    Morphia morphia = new Morphia();
    morphia.getMapper().getOptions().setMapSubPackages(true);
    morphia.mapPackage("io.harness");

    assertThatThrownBy(
        () -> IndexManager.ensureIndexes(INSPECT, persistence.getDatastore(TestIndexEntity.class), morphia))
        .isInstanceOf(IndexManagerInspectException.class);

    IndexManager.ensureIndexes(AUTO, persistence.getDatastore(TestIndexEntity.class), morphia);

    assertThatCode(() -> IndexManager.ensureIndexes(INSPECT, persistence.getDatastore(TestIndexEntity.class), morphia))
        .doesNotThrowAnyException();
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testSubsequenceKeys() {
    BasicDBObject a1 = new BasicDBObject();
    a1.put("a", 1);

    BasicDBObject a_1 = new BasicDBObject();
    a_1.put("a", -1);

    BasicDBObject a1b1 = new BasicDBObject();
    a1b1.put("a", 1);
    a1b1.put("b", 1);

    assertThat(IndexManager.IndexCreator.subsequenceKeys(a1, a_1)).isFalse();
    assertThat(IndexManager.IndexCreator.subsequenceKeys(a_1, a1)).isFalse();

    assertThat(IndexManager.IndexCreator.subsequenceKeys(a1b1, a1)).isTrue();
    assertThat(IndexManager.IndexCreator.subsequenceKeys(a1b1, a_1)).isFalse();

    assertThat(IndexManager.IndexCreator.subsequenceKeys(a1, a1b1)).isFalse();
    assertThat(IndexManager.IndexCreator.subsequenceKeys(a_1, a1b1)).isFalse();
  }
}