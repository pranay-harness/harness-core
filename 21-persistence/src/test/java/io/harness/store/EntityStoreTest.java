package io.harness.store;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;

import io.harness.PersistenceTest;
import io.harness.annotation.StoreIn;
import io.harness.category.element.UnitTests;
import io.harness.persistence.HPersistence;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.ReadPref;
import io.harness.persistence.Store;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mongodb.morphia.AdvancedDatastore;

import java.util.Set;

@StoreIn(name = "foo")
class Dummy implements PersistentEntity {}

public class EntityStoreTest extends PersistenceTest {
  @Inject private HPersistence persistence;

  @Test
  @Category(UnitTests.class)
  public void testGetDatastore() {
    Set<Class> classes = ImmutableSet.<Class>of();
    persistence.register(Store.builder().name("foo").build(), "mongodb://localhost:27017/dummy", classes);
    final AdvancedDatastore datastore = persistence.getDatastore(Dummy.class, ReadPref.CRITICAL);
    assertThat(datastore.getDB().getName()).isEqualTo("dummy");
  }
}
