package io.harness.persistence;

import com.mongodb.DBCollection;
import org.mongodb.morphia.AdvancedDatastore;

public interface HPersistence {
  Store DEFAULT_STORE = Store.builder().name("default").build();

  /**
   * Gets the datastore.
   *
   * @param store the store
   * @param readPref the readPref
   * @return the datastore
   */
  AdvancedDatastore getDatastore(Store store, ReadPref readPref);

  /**
   * Gets the datastore.
   *
   * @param entity the entity
   * @param readPref the readPref
   * @return the datastore
   */
  default AdvancedDatastore getDatastore(Entity entity, ReadPref readPref) {
    if (entity instanceof StoreSelector) {
      return getDatastore(((StoreSelector) entity).getStore(), readPref);
    }
    return getDatastore(DEFAULT_STORE, readPref);
  }

  /**
   * Gets the datastore.
   *
   * @param cls the entity class
   * @param readPref the readPref
   * @return the datastore
   */
  default AdvancedDatastore getDatastore(Class cls, ReadPref readPref) {
    return getDatastore(DEFAULT_STORE, readPref);
  }

  /**
   * Gets the collection.
   *
   * @param collectionName the collection name
   * @param readPref the readPref
   * @return the collection
   */
  DBCollection getCollection(Store store, ReadPref readPref, String collectionName);
}
