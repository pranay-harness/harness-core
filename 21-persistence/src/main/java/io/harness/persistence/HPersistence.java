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
   * @param classes classes this datastore serves
   */
  void register(Store store, String uri, Set<Class> classes);

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
   * Close.
   */
  void close();

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
   * @param entity   the entity
   * @return the key of the entity
   */
  <T extends PersistentEntity> String save(T entity);

  /**
   * Save.
   *
   * @param entityList list of entities to save
   * @return list of keys
   */
  <T extends PersistentEntity> List<String> save(List<T> entityList);

  /**
   * Save ignoring duplicate key errors.
   * This saves any new records and skips existing records
   *
   * @param entityList list of entities to save
   */
  <T extends PersistentEntity> void saveIgnoringDuplicateKeys(List<T> entityList);

  /**
   * Get returns the entity with id.
   *
   * @param cls the class of the entity
   * @param id  the id of the entity
   * @return the t
   */
  <T extends PersistentEntity> T get(Class<T> cls, String id);

  /**
   * Get returns the entity with id.
   *
   * @param <T>      the generic type
   * @param cls      the cls
   * @param id       the id
   * @param readPref the read pref
   * @return the t
   */
  <T extends PersistentEntity> T get(Class<T> cls, String id, ReadPref readPref);

  /**
   * Delete.
   *
   * @param cls  the class of the entity
   * @param id the id
   * @return true, if successful
   */
  <T extends PersistentEntity> boolean delete(Class<T> cls, String id);

  /**
   * Delete.
   *
   * @param query query that selects one or more items to delete
   * @return true, if successful
   */
  <T extends PersistentEntity> boolean delete(Query<T> query);

  /**
   * Delete.
   *
   * @param entity entity to delete
   * @return true, if successful
   */
  <T extends PersistentEntity> boolean delete(T entity);
}
