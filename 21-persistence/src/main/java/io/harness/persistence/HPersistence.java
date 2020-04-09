package io.harness.persistence;

import com.mongodb.DBCollection;
import com.mongodb.MongoSocketOpenException;
import com.mongodb.MongoSocketReadException;
import io.harness.annotation.StoreIn;
import io.harness.exception.ExceptionUtils;
import io.harness.health.HealthMonitor;
import io.harness.persistence.HQuery.QueryChecks;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.FindAndModifyOptions;
import org.mongodb.morphia.query.CountOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface HPersistence extends HealthMonitor {
  Store DEFAULT_STORE = Store.builder().name("default").build();

  /**
   * Register Uri for the datastore.
   *
   * @param store the store
   * @param uri the datastore uri
   */
  void register(Store store, String uri);

  /**
   * Register User provider.
   *
   * @param userProvider user provider
   */
  void registerUserProvider(UserProvider userProvider);

  /**
   * Gets the datastore.
   *
   * @param store    the store
   * @param readPref the readPref
   * @return         the datastore
   */
  AdvancedDatastore getDatastore(Store store);

  /**
   * Gets the datastore.
   *
   * @param entity   the entity
   * @param readPref the readPref
   * @return         the datastore
   */
  default AdvancedDatastore getDatastore(PersistentEntity entity) {
    return getDatastore(entity.getClass());
  }

  Map<Class, Store> getClassStores();

  /**
   * Gets the datastore.
   *
   * @param cls      the entity class
   * @param readPref the readPref
   * @return         the datastore
   */

  default AdvancedDatastore getDatastore(Class cls) {
    return getDatastore(getClassStores().computeIfAbsent(cls, klass -> {
      return Arrays.stream(cls.getDeclaredAnnotations())
          .filter(annotation -> annotation.annotationType().equals(StoreIn.class))
          .map(annotation -> ((StoreIn) annotation).value())
          .map(name -> Store.builder().name(name).build())
          .findFirst()
          .orElseGet(() -> DEFAULT_STORE);
    }));
  }

  /**
   * Gets the collection.
   *
   * @param store          the store
   * @param collectionName the collection name
   * @param readPref       the readPref
   * @return               the collection
   */
  DBCollection getCollection(Store store, String collectionName);

  /**
   * Gets the collection.
   *
   * @param cls            the class of the collection
   * @param readPref       the readPref
   * @return               the collection
   */
  DBCollection getCollection(Class cls);

  /**
   * Gets the collection.
   *
   * @param cls            the class of the collection that indexes should be ensured
   */
  void ensureIndex(Class cls);

  /**
   * Close.
   */
  void close();

  CountOptions upToOne = new CountOptions().limit(1);

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
   * @param <T>          the generic type
   * @param cls          the cls
   * @param queryChecks  the query checks
   * @return             the query
   */
  <T extends PersistentEntity> Query<T> createQuery(Class<T> cls, Set<QueryChecks> queryChecks);

  /**
   * Creates the update operations.
   *
   * @param cls the cls
   * @return the update operations
   */
  <T extends PersistentEntity> UpdateOperations<T> createUpdateOperations(Class<T> cls);

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
   * Insert.
   *
   * @param entity   the entity
   * @return the key of the entity
   */
  <T extends PersistentEntity> String insert(T entity);

  /**
   * Insert.
   *
   * @param entity   the entity
   * @return the key of the entity
   */
  <T extends PersistentEntity> String insertIgnoringDuplicateKeys(T entity);

  /**
   * Get returns the entity with id.
   *
   * @param cls the class of the entity
   * @param id  the id of the entity
   * @return the t
   */
  <T extends PersistentEntity> T get(Class<T> cls, String id);

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

  FindAndModifyOptions upsertReturnNewOptions = new FindAndModifyOptions().upsert(true).returnNew(true);
  FindAndModifyOptions upsertReturnOldOptions = new FindAndModifyOptions().upsert(true).returnNew(false);

  /**
   * Upsert.
   *
   * @param query            the query
   * @param updateOperations the update operations
   * @return the entity
   */
  <T extends PersistentEntity> T upsert(
      Query<T> query, UpdateOperations<T> updateOperations, FindAndModifyOptions options);

  /**
   * Update.
   *
   * @param ent the ent
   * @param ops the ops
   * @return the update results
   */
  <T extends PersistentEntity> UpdateResults update(T ent, UpdateOperations<T> ops);

  /**
   * Update.
   *
   * @param updateQuery      the update query
   * @param updateOperations the update operations
   * @return the update results
   */
  <T extends PersistentEntity> UpdateResults update(Query<T> updateQuery, UpdateOperations<T> updateOperations);

  FindAndModifyOptions returnNewOptions = new FindAndModifyOptions().upsert(false).returnNew(true);
  FindAndModifyOptions returnOldOptions = new FindAndModifyOptions().upsert(false).returnNew(false);

  /**
   * Find and modify.
   *
   * @param query                the query
   * @param updateOperations     the update operations
   * @param findAndModifyOptions the find and modify options
   * @return previous or new entity depending on options
   */
  <T extends PersistentEntity> T findAndModify(
      Query<T> query, UpdateOperations<T> updateOperations, FindAndModifyOptions findAndModifyOptions);

  /**
   * Find and modify data that is system and it should not refresh any of the trackers.
   *
   * @param query                the query
   * @param updateOperations     the update operations
   * @param findAndModifyOptions the find and modify options
   * @return previous or new entity depending on options
   */
  <T extends PersistentEntity> T findAndModifySystemData(
      Query<T> query, UpdateOperations<T> updateOperations, FindAndModifyOptions findAndModifyOptions);

  /**
   * Merge.
   *
   * @param entity   the entity to merge
   * @return the key of the entity
   */
  <T extends PersistentEntity> String merge(T entity);

  int RETRIES = 3;

  interface Executor<R> {
    R execute();
  }

  static <R> R retry(Executor<R> executor) {
    for (int i = 1; i < RETRIES; ++i) {
      try {
        return executor.execute();
      } catch (MongoSocketOpenException | MongoSocketReadException ignore) {
        continue;
      } catch (RuntimeException exception) {
        if (ExceptionUtils.cause(MongoSocketOpenException.class, exception) != null) {
          continue;
        }
        if (ExceptionUtils.cause(MongoSocketReadException.class, exception) != null) {
          continue;
        }
        throw exception;
      }
    }
    // one last try
    return executor.execute();
  }
}
