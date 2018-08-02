package software.wings.dl;

import com.mongodb.DBCollection;
import com.mongodb.client.gridfs.GridFSBucket;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.FindAndModifyOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
import software.wings.beans.Base;
import software.wings.beans.ReadPref;
import software.wings.dl.HQuery.QueryChecks;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The Interface WingsPersistence.
 */
public interface WingsPersistence {
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
   * Get t.
   *
   * @param <T>      the type parameter
   * @param cls      the cls
   * @param appId    the app id
   * @param id       the id
   * @param readPref the read pref
   * @return the t
   */
  <T extends Base> T get(Class<T> cls, String appId, String id, ReadPref readPref);

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
   * Find and modify t.
   *
   * @param <T>                  the type parameter
   * @param query                the query
   * @param updateOperations     the update operations
   * @param findAndModifyOptions the find and modify options
   * @return the t
   */
  <T extends Base> T findAndModify(
      Query<T> query, UpdateOperations<T> updateOperations, FindAndModifyOptions findAndModifyOptions);

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

  /**
   * Gets count.
   *
   * @param <T> the type parameter
   * @param cls the cls
   * @param req the req
   * @return the count
   */
  <T> long getCount(Class<T> cls, PageRequest<T> req);

  /**
   * Creates the update operations.
   *
   * @param <T> the generic type
   * @param cls the cls
   * @return the update operations
   */
  <T> UpdateOperations<T> createUpdateOperations(Class<T> cls);

  /**
   * Upsert t.
   *
   * @param <T>              the type parameter
   * @param query            the query
   * @param updateOperations the update operations
   * @return the t
   */
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
   * Update fields
   *
   * @param <T>            the type parameter
   * @param cls            the cls
   * @param entityId       the entity id
   * @param keyValuePairs  the key value pairs
   * @param fieldsToRemove the fields to remove
   */
  <T> void updateFields(Class<T> cls, String entityId, Map<String, Object> keyValuePairs, Set<String> fieldsToRemove);

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
   * Delete with account id
   * @param accountId
   * @param cls
   * @param uuid
   * @param <T>
   * @return
   */
  <T extends Base> boolean delete(String accountId, Class<T> cls, String uuid);

  /**
   * Delete boolean.
   *
   * @param <T>   the type parameter
   * @param cls   the cls
   * @param appId the app id
   * @param uuid  the uuid
   * @return the boolean
   */
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

  /**
   * Query page response.
   *
   * @param <T>          the type parameter
   * @param cls          the cls
   * @param req          the req
   * @param queryChecks  the query checks
   * @return             the page response
   */
  <T> PageResponse<T> query(Class<T> cls, PageRequest<T> req, Set<QueryChecks> queryChecks);

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
   * @param <T>          the generic type
   * @param cls          the cls
   * @param queryChecks  the query checks
   * @return             the query
   */
  <T> Query<T> createQuery(Class<T> cls, Set<QueryChecks> queryChecks);

  /**
   * Creates the query.
   *
   * @param <T>      the generic type
   * @param cls      the cls
   * @param readPref the read pref
   * @return         the query
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
   * Creates a query and runs the authFilter to it.
   * This api is preferred over createQuery() api.
   *
   * @param collectionClass the collection class
   * @return query
   */
  <T> Query<T> createAuthorizedQuery(Class<T> collectionClass);

  /**
   * Creates a query and runs the authFilter to it.
   * This api is preferred over createQuery() api.
   * This overloaded api is used in the case where the validation needs to be disabled.
   * This is needed for the following case:
   * 1) If the query looks up a field which is part of an embedded object,
   * but that embedded object is a base class and if we are referring to a field from the derived class, validation
   * fails right now. This is a stop gap solution until that is fixed.
   *
   * @param  collectionClass   the collection class
   * @param  disableValidation the disable validation
   * @return query
   */
  Query createAuthorizedQuery(Class collectionClass, boolean disableValidation);
}
