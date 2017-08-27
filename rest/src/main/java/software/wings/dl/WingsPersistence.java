package software.wings.dl;

import com.mongodb.DBCollection;
import com.mongodb.client.gridfs.GridFSBucket;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
import software.wings.beans.Base;
import software.wings.beans.ReadPref;

import java.util.List;
import java.util.Map;

/**
 * The Interface WingsPersistence.
 */
public interface WingsPersistence {
  /**
   * List.
   *
   * @param <T> the generic type
   * @param cls the cls
   * @return the list
   */
  <T extends Base> List<T> list(Class<T> cls);

  /**
   * List.
   *
   * @param <T>      the generic type
   * @param cls      the cls
   * @param readPref the read pref
   * @return the list
   */
  <T extends Base> List<T> list(Class<T> cls, ReadPref readPref);

  /**
   * Gets the.
   *
   * @param <T> the generic type
   * @param cls the cls
   * @param req the req
   * @return the t
   */
  <T extends Base> T get(Class<T> cls, PageRequest<T> req);

  /**
   * Gets the.
   *
   * @param <T>      the generic type
   * @param cls      the cls
   * @param req      the req
   * @param readPref the read pref
   * @return the t
   */
  <T extends Base> T get(Class<T> cls, PageRequest<T> req, ReadPref readPref);

  /**
   * Gets the.
   *
   * @param <T> the generic type
   * @param cls the cls
   * @param id  the id
   * @return the t
   */
  <T extends Base> T get(Class<T> cls, String id);

  /**
   * Gets the.
   *
   * @param <T>   the generic type
   * @param cls   the cls
   * @param appId the app id
   * @param id    the id
   * @return the t
   */
  <T extends Base> T get(Class<T> cls, String appId, String id);

  /**
   * Gets the.
   *
   * @param <T>      the generic type
   * @param cls      the cls
   * @param id       the id
   * @param readPref the read pref
   * @return the t
   */
  <T extends Base> T get(Class<T> cls, String id, ReadPref readPref);

  /**
   * Find one by query t.
   *
   * @param <T>   the type parameter
   * @param query the query
   * @return the t
   */
  <T extends Base> T executeGetOneQuery(Query<T> query);

  /**
   * Merge string.
   *
   * @param <T> the type parameter
   * @param t   the t
   * @return the string
   */
  <T extends Base> String merge(T t);

  /**
   * Save.
   *
   * @param <T>   the generic type
   * @param tList the t list
   * @return the list
   */
  <T extends Base> List<String> save(List<T> tList);

  /**
   * Save ignoring duplicate key errors.
   * This saves any new records and skips existing records
   *
   * @param <T>   the generic type
   * @param tList the t list
   * @return the list
   */
  <T extends Base> List<String> saveIgnoringDuplicateKeys(List<T> tList);

  /**
   * Save.
   *
   * @param <T> the generic type
   * @param t   the t
   * @return the string
   */
  <T extends Base> String save(T t);

  /**
   * Save and get.
   *
   * @param <T> the generic type
   * @param cls the cls
   * @param t   the t
   * @return the t
   */
  <T extends Base> T saveAndGet(Class<T> cls, T t);

  <T> PageResponse<T> query(Class<T> cls, PageRequest<T> req, ReadPref readPref, boolean disableValidation);

  /**
   * Creates the update operations.
   *
   * @param <T> the generic type
   * @param cls the cls
   * @return the update operations
   */
  <T> UpdateOperations<T> createUpdateOperations(Class<T> cls);

  <T> T upsert(Query<T> query, UpdateOperations<T> updateOperations);

  /**
   * Update.
   *
   * @param <T> the generic type
   * @param ent the ent
   * @param ops the ops
   * @return the update results
   */
  <T extends Base> UpdateResults update(T ent, UpdateOperations<T> ops);

  /**
   * Update.
   *
   * @param <T>              the generic type
   * @param updateQuery      the update query
   * @param updateOperations the update operations
   * @return the update results
   */
  <T> UpdateResults update(Query<T> updateQuery, UpdateOperations<T> updateOperations);

  /**
   * Update field.
   *
   * @param <T>       the generic type
   * @param cls       the cls
   * @param entityId  the entity id
   * @param fieldName the field name
   * @param value     the value
   */
  <T> void updateField(Class<T> cls, String entityId, String fieldName, Object value);

  /**
   * Update fields.
   *
   * @param <T>           the generic type
   * @param cls           the cls
   * @param entityId      the entity id
   * @param keyValuePairs the key value pairs
   */
  <T> void updateFields(Class<T> cls, String entityId, Map<String, Object> keyValuePairs);

  /**
   * Update fields.
   *
   * @param <T>           the generic type
   * @param cls           the cls
   * @param entityId      the entity id
   * @param encryptionKey the account id
   * @param keyValuePairs the key value pairs
   */
  <T> void updateFields(Class<T> cls, String entityId, Map<String, Object> keyValuePairs, String encryptionKey);

  /**
   * Delete.
   *
   * @param <T>  the generic type
   * @param cls  the cls
   * @param uuid the uuid
   * @return true, if successful
   */
  <T extends Base> boolean delete(Class<T> cls, String uuid);

  <T extends Base> boolean delete(Class<T> cls, String appId, String uuid);

  /**
   * Delete.
   *
   * @param <T>   the generic type
   * @param query the query
   * @return true, if successful
   */
  <T extends Base> boolean delete(Query<T> query);

  /**
   * Delete.
   *
   * @param <T>    the generic type
   * @param entity the entity
   * @return true, if successful
   */
  <T extends Base> boolean delete(T entity);

  /**
   * Query.
   *
   * @param <T> the generic type
   * @param cls the cls
   * @param req the req
   * @return the page response
   */
  <T> PageResponse<T> query(Class<T> cls, PageRequest<T> req);

  <T> PageResponse<T> query(Class<T> cls, PageRequest<T> req, boolean disableValidation);

  /**
   * Query.
   *
   * @param <T>      the generic type
   * @param cls      the cls
   * @param req      the req
   * @param readPref the read pref
   * @return the page response
   */
  <T> PageResponse<T> query(Class<T> cls, PageRequest<T> req, ReadPref readPref);

  /**
   * Creates the query.
   *
   * @param <T> the generic type
   * @param cls the cls
   * @return the query
   */
  <T> Query<T> createQuery(Class<T> cls);

  /**
   * Creates the query.
   *
   * @param <T>      the generic type
   * @param cls      the cls
   * @param readPref the read pref
   * @return the query
   */
  <T> Query<T> createQuery(Class<T> cls, ReadPref readPref);

  /**
   * Gets the or create grid fs bucket.
   *
   * @param bucketName the bucket name
   * @return the or create grid fs bucket
   */
  GridFSBucket getOrCreateGridFSBucket(String bucketName);

  /**
   * Gets datastore.
   *
   * @return the datastore
   */
  Datastore getDatastore();

  /**
   * Gets the collection.
   *
   * @param collectionName the collection name
   * @return the collection
   */
  DBCollection getCollection(String collectionName);

  /**
   * Close.
   */
  void close();

  /**
   * Only to be used in testing encryption.
   *
   * @param <T> the generic type
   * @param cls the cls
   * @param id  the id
   * @return the t
   */
  <T extends Base> T getWithoutDecryptingTestOnly(Class<T> cls, String id);
}
