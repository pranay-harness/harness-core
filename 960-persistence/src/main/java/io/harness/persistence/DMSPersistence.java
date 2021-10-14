package io.harness.persistence;

import io.harness.beans.PageRequest;

import java.util.Map;
import java.util.Set;
import org.mongodb.morphia.query.Query;

public interface DMSPersistence extends HPersistence {
  /**
   * Gets the.
   *
   * @param <T>   the generic type
   * @param cls   the cls
   * @param appId the app id
   * @param id    the id
   * @return the t
   */
  <T extends PersistentEntity> T getWithAppId(Class<T> cls, String appId, String id);

  /**
   * Update field.
   *
   * @param <T>       the generic type
   * @param cls       the cls
   * @param entityId  the entity id
   * @param fieldName the field name
   * @param value     the value
   */
  <T extends PersistentEntity> void updateField(Class<T> cls, String entityId, String fieldName, Object value);

  /**
   * Update fields.
   *
   * @param <T>           the generic type
   * @param cls           the cls
   * @param entityId      the entity id
   * @param keyValuePairs the key value pairs
   */
  <T extends PersistentEntity> void updateFields(Class<T> cls, String entityId, Map<String, Object> keyValuePairs);

  /**
   * Update fields
   *
   * @param <T>            the type parameter
   * @param cls            the cls
   * @param entityId       the entity id
   * @param keyValuePairs  the key value pairs
   * @param fieldsToRemove the fields to remove
   */
  <T extends PersistentEntity> void updateFields(
      Class<T> cls, String entityId, Map<String, Object> keyValuePairs, Set<String> fieldsToRemove);

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

  // convertToQuery converts a PageRequest object to a Query object with the same search filters.
  <T extends PersistentEntity> Query<T> convertToQuery(Class<T> cls, PageRequest<T> req);
  <T extends PersistentEntity> Query<T> convertToQuery(
      Class<T> cls, PageRequest<T> req, Set<HQuery.QueryChecks> queryChecks);

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
