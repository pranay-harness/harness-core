package io.harness.persistence;

import com.mongodb.DBCollection;
import io.harness.annotation.StoreIn;
import io.harness.persistence.HQuery.QueryChecks;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.query.Query;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
  default AdvancedDatastore getDatastore(PersistentEntity entity, ReadPref readPref) {
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

  /**
   * Creates the query.
   *
   * @param <T> the generic type
   * @param cls the cls
   * @return the query
   */
  <T extends PersistentEntity> Query<T> createQuery(Class<T> cls);

  /**
   * Creates the query.
   *
   * @param <T>      the generic type
   * @param cls      the cls
   * @param readPref the read pref
   * @return         the query
   */
  <T extends PersistentEntity> Query<T> createQuery(Class<T> cls, ReadPref readPref);

  /**
   * Creates the query.
   *
   * @param <T>          the generic type
   * @param cls          the cls
   * @param queryChecks  the query checks
   * @return             the query
   */
  <T extends PersistentEntity> Query<T> createQuery(Class<T> cls, Set<QueryChecks> queryChecks);

  /**
   * Creates the query.
   *
   * @param <T>          the generic type
   * @param cls          the cls
   * @param readPref     the read pref
   * @param queryChecks  the query checks
   * @return             the query
   */
  <T extends PersistentEntity> Query<T> createQuery(Class<T> cls, ReadPref readPref, Set<QueryChecks> queryChecks);

  /**
   * Save.
   *
   * @param tList list of entities to save
   * @return list of keys
   */
  <T extends PersistentEntity> List<String> save(List<T> tList);

  /**
   * Save ignoring duplicate key errors.
   * This saves any new records and skips existing records
   *
   * @param tList list of entities to save
   * @return list of keys of new entities
   */
  <T extends PersistentEntity> List<String> saveIgnoringDuplicateKeys(List<T> tList);

  /**
   * Save.
   *
   * @param entity   the entity
   * @return the key of the entity
   */
  <T extends PersistentEntity> String save(T t);
}
