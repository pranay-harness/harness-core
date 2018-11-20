package software.wings.dl;

import com.mongodb.client.gridfs.GridFSBucket;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.persistence.HPersistence;
import io.harness.persistence.HQuery.QueryChecks;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.ReadPref;
import io.harness.persistence.Store;
import org.mongodb.morphia.FindAndModifyOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
import software.wings.beans.Base;

import java.util.Map;
import java.util.Set;

/**
 * The Interface WingsPersistence.
 */
public interface WingsPersistence extends HPersistence {
  Store LOCKS_STORE = Store.builder().name("locks").build();

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
   * Delete with account id
   * @param accountId
   * @param cls
   * @param uuid
   * @param <T>
   * @return
   */
  <T extends PersistentEntity> boolean delete(String accountId, Class<T> cls, String uuid);

  /**
   * Delete boolean.
   *
   * @param <T>   the type parameter
   * @param cls   the cls
   * @param appId the app id
   * @param uuid  the uuid
   * @return the boolean
   */
  <T extends PersistentEntity> boolean delete(Class<T> cls, String appId, String uuid);

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
   * Gets the or create grid fs bucket.
   *
   * @param bucketName the bucket name
   * @return the or create grid fs bucket
   */
  GridFSBucket getOrCreateGridFSBucket(String bucketName);

  /**
   * Creates a query and runs the authFilter to it.
   * This api is preferred over createQuery() api.
   *
   * @param collectionClass the collection class
   * @return query
   */
  <T extends PersistentEntity> Query<T> createAuthorizedQuery(Class<T> collectionClass);

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
