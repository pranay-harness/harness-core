package io.harness.store;

import static io.harness.rule.OwnerRule.GEORGE;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.PersistenceTest;
import io.harness.annotation.StoreIn;
import io.harness.category.element.UnitTests;
import io.harness.persistence.HPersistence;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.Store;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mongodb.morphia.AdvancedDatastore;

@StoreIn("foo")
class Dummy implements PersistentEntity {}

public class EntityStoreTest extends PersistenceTest {
  @Inject private HPersistence persistence;

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testGetDatastore() {
    persistence.register(Store.builder().name("foo").build(), "mongodb://localhost:27017/dummy");
    final AdvancedDatastore datastore = persistence.getDatastore(Dummy.class);
    assertThat(datastore.getDB().getName()).isEqualTo("dummy");
  }
}
