package io.harness.store;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.PersistenceTest;
import io.harness.annotation.StoreIn;
import io.harness.persistence.Entity;
import io.harness.persistence.HPersistence;
import io.harness.persistence.ReadPref;
import io.harness.persistence.Store;
import org.junit.Test;
import org.mongodb.morphia.AdvancedDatastore;

@StoreIn(name = "foo")
class Dummy extends Entity {}

public class EntityStoreTest extends PersistenceTest {
  @Inject private HPersistence persistence;

  @Test
  public void testGetDatastore() {
    persistence.register(Store.builder().name("foo").build(), "mongodb://localhost:27017/dummy");
    final AdvancedDatastore datastore = persistence.getDatastore(Dummy.class, ReadPref.CRITICAL);
    assertThat(datastore.getDB().getName()).isEqualTo("dummy");
  }
}
