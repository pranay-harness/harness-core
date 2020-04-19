package io.harness.mongo;

import static io.harness.mongo.IndexManager.Mode.AUTO;
import static io.harness.mongo.IndexManager.Mode.INSPECT;
import static io.harness.rule.OwnerRule.GEORGE;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.inject.Inject;

import io.harness.PersistenceTest;
import io.harness.category.element.UnitTests;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import io.harness.rule.RealMongo;
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
}