package software.wings.dl;

import com.mongodb.DBCollection;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
import software.wings.beans.Base;
import software.wings.beans.ReadPref;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

// TODO: Auto-generated Javadoc

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
   * Save.
   *
   * @param <T>   the generic type
   * @param tList the t list
   * @return the list
   */
  <T extends Base> List<String> save(List<T> tList);

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

  /**
   * Creates the update operations.
   *
   * @param <T> the generic type
   * @param cls the cls
   * @return the update operations
   */
  <T> UpdateOperations<T> createUpdateOperations(Class<T> cls);

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
   */
  <T> void update(Query<T> updateQuery, UpdateOperations<T> updateOperations);

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
   * Adds the to list.
   *
   * @param <T>           the generic type
   * @param cls           the cls
   * @param appId         the app id
   * @param entityId      the entity id
   * @param listFieldName the list field name
   * @param object        the object
   */
  <T> void addToList(Class<T> cls, String appId, String entityId, String listFieldName, Object object);

  /**
   * Adds the to list.
   *
   * @param <T>           the generic type
   * @param cls           the cls
   * @param appId         the app id
   * @param entityId      the entity id
   * @param query         the query
   * @param listFieldName the list field name
   * @param object        the object
   * @return true, if successful
   */
  <T> boolean addToList(
      Class<T> cls, String appId, String entityId, Query<T> query, String listFieldName, Object object);

  /**
   * Adds the to list.
   *
   * @param <T>           the generic type
   * @param cls           the cls
   * @param entityId      the entity id
   * @param listFieldName the list field name
   * @param object        the object
   */
  <T> void addToList(Class<T> cls, String entityId, String listFieldName, Object object);

  /**
   * Delete from list.
   *
   * @param <T>           the generic type
   * @param cls           the cls
   * @param entityId      the entity id
   * @param listFieldName the list field name
   * @param object        the object
   */
  <T> void deleteFromList(Class<T> cls, String entityId, String listFieldName, Object object);

  /**
   * Delete.
   *
   * @param <T>  the generic type
   * @param cls  the cls
   * @param uuid the uuid
   * @return true, if successful
   */
  <T extends Base> boolean delete(Class<T> cls, String uuid);

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
   * Upload from stream.
   *
   * @param bucketName the bucket name
   * @param options    the options
   * @param filename   the filename
   * @param in         the in
   * @return the string
   */
  String uploadFromStream(String bucketName, GridFSUploadOptions options, String filename, InputStream in);

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
}
