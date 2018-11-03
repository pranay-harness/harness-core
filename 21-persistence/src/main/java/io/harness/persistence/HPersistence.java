package io.harness.persistence;

import com.mongodb.DBCollection;
import io.harness.annotation.StoreIn;
import org.mongodb.morphia.AdvancedDatastore;

import java.util.Arrays;
import java.util.Map;

public interface HPersistence {
  Store DEFAULT_STORE = Store.builder().name("default").build();

  /**
   * Register Uri for the datastore.
   *
   * @param store the store
   * @param uri the datastore uri
   */
  void register(Store store, String uri);

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
    return getDatastore(entity.getClass(), readPref);
  }

  Map<Class, Store> getClassStores();

  /**
   * Gets the datastore.
   *
   * @param cls the entity class
   * @param readPref the readPref
   * @return the datastore
   */

  default AdvancedDatastore getDatastore(Class cls, ReadPref readPref) {
    return getDatastore(getClassStores().computeIfAbsent(cls, klass -> {
      return Arrays.stream(cls.getDeclaredAnnotations())
          .filter(annotation -> annotation.annotationType().equals(StoreIn.class))
          .map(annotation -> ((StoreIn) annotation).name())
          .map(name -> Store.builder().name(name).build())
          .findFirst()
          .orElseGet(() -> DEFAULT_STORE);
    }), readPref);
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
