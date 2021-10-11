package software.wings.dl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.harness.beans.*;
import io.harness.encryption.EncryptionReflectUtils;
import io.harness.eraro.ErrorCode;
import io.harness.exception.EncryptDecryptException;
import io.harness.exception.WingsException;
import io.harness.mongo.MongoPersistence;
import io.harness.mongo.PageController;
import io.harness.persistence.*;


import com.google.inject.name.Named;
import io.dropwizard.lifecycle.Managed;
import io.harness.reflection.ReflectionUtils;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.DatastoreImpl;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.Base;
import software.wings.beans.ServiceVariable;
import software.wings.beans.SettingAttribute;
import software.wings.beans.User;
import software.wings.security.UserPermissionInfo;
import software.wings.security.UserRequestContext;
import software.wings.security.UserThreadLocal;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.SettingVariableTypes;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.Callable;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.encryption.EncryptionReflectUtils.getEncryptedRefField;
import static io.harness.encryption.EncryptionReflectUtils.isSecretReference;
import static io.harness.exception.WingsException.USER;
import static io.harness.persistence.HQuery.allChecks;
import static io.harness.persistence.HQuery.excludeAuthority;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.utils.WingsReflectionUtils.*;

@Singleton
@Slf4j
public class DMSMongoPersistence extends MongoPersistence implements WingsPersistence, Managed {

  @Inject private SecretManager secretManager;

  @Inject
  public DMSMongoPersistence(@Named("primaryDatastore") AdvancedDatastore primaryDatastore) {
    super(primaryDatastore);
  }

  @Override
  public void start() throws Exception {
    // Do nothing
  }

  @Override
  public void stop() throws Exception {
    close();
  }

  @Override
  public <T extends PersistentEntity> T getWithAppId(Class<T> cls, String appId, String id) {
    return createQuery(cls).filter("appId", appId).filter(ID_KEY, id).get();
  }

  @Override
  public <T extends PersistentEntity> String save(T object) {
    Optional<T> savedObjectOptional = getSavedEntity(object);
    T savedEntity = savedObjectOptional.orElse(null);
    boolean isEncryptable = encryptIfNecessary(object, savedEntity);
    String key = super.save(object);
    if (key != null && isEncryptable) {
      updateEncryptionReferencesIfNecessary(object, key, savedEntity);
    }
    return key;
  }

  @Override
  public <T extends PersistentEntity> void saveIgnoringDuplicateKeys(List<T> ts) {
    for (Iterator<T> iterator = ts.iterator(); iterator.hasNext();) {
      T t = iterator.next();
      if (t != null) {
        encryptIfNecessary(t, null);
      }
    }

    super.saveIgnoringDuplicateKeys(ts);
  }



  @Override
  public <T extends Base> T saveAndGet(Class<T> cls, T object) {
    Object id = save(object);
    Query<T> query = createQuery(cls).filter(ID_KEY, id);
    if (object.getShardKeys() != null) {
      object.getShardKeys().keySet().forEach(key -> query.filter(key, object.getShardKeys().get(key)));
    }
    return query.get();
  }



  @Override
  public <T extends PersistentEntity> void updateField(Class<T> cls, String entityId, String fieldName, Object value) {
    Map<String, Object> keyValuePairs = new HashMap<>();
    keyValuePairs.put(fieldName, value);
    updateFields(cls, entityId, keyValuePairs);
  }

  @Override
  public <T extends PersistentEntity> void updateFields(
          Class<T> cls, String entityId, Map<String, Object> keyValuePairs) {
    updateFields(cls, entityId, keyValuePairs, Collections.emptySet());
  }

  @Override
  public <T extends PersistentEntity> void updateFields(
          Class<T> cls, String entityId, Map<String, Object> keyValuePairs, Set<String> fieldsToRemove) {
    final AdvancedDatastore datastore = getDatastore(cls);

    Query<T> query = datastore.createQuery(cls).filter(ID_KEY, entityId);
    UpdateOperations<T> operations = datastore.createUpdateOperations(cls);
    T savedObject = datastore.get(cls, entityId);

    boolean encryptable = EncryptableSetting.class.isAssignableFrom(cls);
    List<Field> fieldsWithEncryptedAnnotation = EncryptionReflectUtils.getEncryptedFields(cls);
    boolean shouldUpdateSecretParents = false;

    try {
      for (Map.Entry<String, Object> entry : keyValuePairs.entrySet()) {
        Object value = entry.getValue();

        if (value instanceof EncryptableSetting) {
          EncryptableSetting encryptableSetting = (EncryptableSetting) value;
          EncryptableSetting savedEncryptableSetting = savedObject != null
                  ? (EncryptableSetting) ReflectionUtils.getFieldValue(savedObject, entry.getKey())
                  : null;
          setEncryptedFields(encryptableSetting);
          encryptPlainTextSecrets(encryptableSetting, savedEncryptableSetting);
          shouldUpdateSecretParents = true;
        } else if (encryptable && savedObject != null) {
          EncryptableSetting encryptableSetting = (EncryptableSetting) savedObject;
          Optional<Field> f = fieldsWithEncryptedAnnotation.stream()
                  .filter(field -> field.getName().equals(entry.getKey()))
                  .findFirst();

          if (f.isPresent() && isEncryptedFieldDuringUpdate(f.get(), encryptableSetting, keyValuePairs)) {
            Field encryptedField = f.get();
            Field encryptedRefField = getEncryptedRefField(encryptedField, encryptableSetting);
            boolean isReference = isSecretReference(encryptedField);
            if (isReference) {
              operations.set(encryptedRefField.getName(), value);
            } else {
              String encryptedId =
                      encryptPlainTextSecret(encryptedField, (char[]) value, encryptableSetting, encryptableSetting);
              operations.set(encryptedRefField.getName(), encryptedId);
            }
            operations.unset(encryptedField.getName());
            continue;
          }
          shouldUpdateSecretParents = true;
        }
        operations.set(entry.getKey(), value);
      }

      for (String fieldToRemove : fieldsToRemove) {
        if (encryptable && savedObject != null) {
          EncryptableSetting encryptableSetting = (EncryptableSetting) savedObject;
          Optional<Field> f =
                  fieldsWithEncryptedAnnotation.stream().filter(field -> field.getName().equals(fieldToRemove)).findFirst();
          if (f.isPresent()) {
            Field encryptedRefField = getEncryptedRefField(f.get(), encryptableSetting);
            operations.unset(encryptedRefField.getName());
          }
          shouldUpdateSecretParents = true;
        }
        operations.unset(fieldToRemove);
      }
    } catch (IllegalAccessException ex) {
      throw new EncryptDecryptException("Failed to encrypt secret due to illegal access exception", ex);
    }

    update(query, operations);
    T updatedObject = datastore.get(cls, entityId);
    if (updatedObject != null && shouldUpdateSecretParents) {
      updateEncryptionReferencesIfNecessary(updatedObject, entityId, savedObject);
    }
  }
  @Override
  public <T extends PersistentEntity> boolean delete(Class<T> cls, String uuid) {
    if (cls.equals(SettingAttribute.class) || EncryptableSetting.class.isAssignableFrom(cls)) {
      Query<T> query = createQuery(cls, excludeAuthority).filter(ID_KEY, uuid);
      return delete(query);
    }

    return super.delete(cls, uuid);
  }

  @Override
  public <T extends PersistentEntity> boolean delete(String accountId, Class<T> cls, String uuid) {
    Query<T> query = getDatastore(DEFAULT_STORE).createQuery(cls).filter(ID_KEY, uuid).filter("accountId", accountId);
    return delete(query);
  }

  @Override
  public <T extends PersistentEntity> boolean delete(Class<T> cls, String appId, String uuid) {
    Query<T> query = getDatastore(cls).createQuery(cls).filter(ID_KEY, uuid).filter("appId", appId);
    return delete(query);
  }

  @Override
  public <T extends PersistentEntity> boolean delete(Query<T> query) {
    try (HIterator<T> records = new HIterator<>(query.fetch())) {
      while (records.hasNext()) {
        deleteEncryptionReferenceIfNecessary(records.next());
      }
    }
    return super.delete(query);
  }

  @Override
  public <T extends PersistentEntity> boolean delete(T entity) {
    deleteEncryptionReferenceIfNecessary(entity);
    return super.delete(entity);
  }

  @Override
  public <T extends PersistentEntity> Query<T> convertToQuery(Class<T> cls, PageRequest<T> req) {
    return convertToQuery(cls, req, allChecks);
  }

  @Override
  public <T extends PersistentEntity> Query<T> convertToQuery(Class<T> cls, PageRequest<T> req, Set<HQuery.QueryChecks> queryChecks) {
    AdvancedDatastore advancedDatastore = getDatastore(cls);
    Query<T> query = advancedDatastore.createQuery(cls);
    authorizeQuery(cls, query);

    ((HQuery) query).setQueryChecks(queryChecks);
    Mapper mapper = ((DatastoreImpl) advancedDatastore).getMapper();

    return PageController.applyPageRequest(advancedDatastore, query, req, cls, mapper);
  }

  @Override
  public <T extends Base> List<T> getAllEntities(PageRequest<T> pageRequest, Callable<PageResponse<T>> callable) {
    return null;
  }

  @Override
  public <T extends PersistentEntity> Query<T> createAuthorizedQuery(Class<T> collectionClass) {
    return null;
  }

  @Override
  public Query createAuthorizedQuery(Class collectionClass, boolean disableValidation) {
    return null;
  }

  @Override
  public AdvancedDatastore getDatastore(PersistentEntity entity) {
    return null;
  }

  @Override
  public AdvancedDatastore getDatastore(Class cls) {
    return null;
  }

  @Override
  public <T extends PersistentEntity> void upsert(Query<T> query, UpdateOperations<T> updateOperations) {

  }

  private <T extends PersistentEntity> Optional<T> getSavedEntity(@NonNull T entity) {
    Optional<String> entityId =
            entity instanceof UuidAware ? Optional.ofNullable(((UuidAware) entity).getUuid()) : Optional.empty();
    return entityId.flatMap(uuid -> Optional.ofNullable(getDatastore(entity).get((Class<T>) entity.getClass(), uuid)));
  }

  private boolean isEncryptedFieldDuringUpdate(
          Field f, EncryptableSetting savedObject, Map<String, Object> keyValuePairs) {
    List<Field> encryptedFields = savedObject.getEncryptedFields();
    if (savedObject.getClass().equals(ServiceVariable.class)) {
      return keyValuePairs.get("type") == ServiceVariable.Type.ENCRYPTED_TEXT;
    }
    return encryptedFields.contains(f);
  }

  private <T extends PersistentEntity> boolean encryptIfNecessary(@NonNull T entity, T savedEntity) {
    try {
      Optional<EncryptableSetting> encryptableSettingOptional = getEncryptableSetting(entity);

      if (encryptableSettingOptional.isPresent()) {
        EncryptableSetting encryptableSetting = encryptableSettingOptional.get();
        setEncryptedFields(encryptableSetting);
        Optional<EncryptableSetting> savedEncryptableSettingOptional =
                savedEntity != null ? getEncryptableSetting(savedEntity) : Optional.empty();
        EncryptableSetting savedEncryptableSetting = savedEncryptableSettingOptional.orElse(null);
        encryptPlainTextSecrets(encryptableSetting, savedEncryptableSetting);
        return true;
      }
    } catch (IllegalAccessException e) {
      throw new EncryptDecryptException(
              String.format(
                      "Illegal access exception while accessing the encrypted fields of object of Class %s while encryption",
                      entity.getClass()),
              e);
    }
    return false;
  }

  private void setEncryptedFields(@NonNull EncryptableSetting object) throws IllegalAccessException {
    List<Field> encryptedFields = Optional.ofNullable(object.getEncryptedFields()).orElseGet(Collections::emptyList);
    for (Field encryptedField : encryptedFields) {
      encryptedField.setAccessible(true);
      Field encryptedRefField = getEncryptedRefField(encryptedField, object);
      encryptedRefField.setAccessible(true);
      boolean isReference = EncryptionReflectUtils.isSecretReference(encryptedField);
      Optional<char[]> secretOptional = Optional.ofNullable((char[]) encryptedField.get(object));

      if (isReference && secretOptional.isPresent()) {
        encryptedRefField.set(object, String.valueOf(secretOptional.get()));
        encryptedField.set(object, null);
      }

      Optional<String> secretIdOptional = Optional.ofNullable((String) encryptedRefField.get(object));

      if (secretIdOptional.isPresent() && isSetByYaml(secretIdOptional.get())) {
        Optional<EncryptedData> encryptedDataOptional = Optional.ofNullable(
                secretManager.getEncryptedDataFromYamlRef(secretIdOptional.get(), object.getAccountId()));
        EncryptedData encryptedData = encryptedDataOptional.<EncryptDecryptException>orElseThrow(
                () -> { throw new EncryptDecryptException("The yaml reference is not valid."); });
        encryptedRefField.set(object, encryptedData.getUuid());
      }
    }
  }

  private Optional<EncryptedData> getEncryptedDataFromField(
          @NonNull EncryptableSetting object, @NonNull Field encryptedRefField) throws IllegalAccessException {
    Optional<String> secretIdOptional = Optional.ofNullable((String) encryptedRefField.get(object));
    return secretIdOptional.flatMap(secretId -> Optional.ofNullable(get(EncryptedData.class, secretId)));
  }

  private void encryptPlainTextSecrets(@NonNull EncryptableSetting object, EncryptableSetting savedObject)
          throws IllegalAccessException {
    List<Field> encryptedFields = Optional.ofNullable(object.getEncryptedFields()).orElseGet(Collections::emptyList);
    for (Field encryptedField : encryptedFields) {
      encryptedField.setAccessible(true);
      Field encryptedRefField = getEncryptedRefField(encryptedField, object);
      encryptedRefField.setAccessible(true);
      Optional<char[]> secretOptional = Optional.ofNullable((char[]) encryptedField.get(object));

      if (secretOptional.isPresent() && isNotEmpty(secretOptional.get())) {
        char[] secret = secretOptional.get();
        String encryptedId = encryptPlainTextSecret(encryptedField, secret, object, savedObject);
        encryptedRefField.set(object, encryptedId);
        encryptedField.set(object, null);
      }
    }
  }

  private String encryptPlainTextSecret(@NonNull Field encryptedField, @NonNull char[] secret,
                                        @NonNull EncryptableSetting object, EncryptableSetting savedObject) throws IllegalAccessException {
    Field encryptedRefField = getEncryptedRefField(encryptedField, object);
    encryptedRefField.setAccessible(true);
    Optional<EncryptedData> savedSecretOptional =
            savedObject == null ? Optional.empty() : getEncryptedDataFromField(savedObject, encryptedRefField);

    String encryptedId;
    if (savedSecretOptional.isPresent()) {
      encryptedId = updateSecret(object.getAccountId(), savedSecretOptional.get(), secret);
    } else {
      encryptedId = createSecret(object.getAccountId(), secret, object.getSettingType());
    }
    return encryptedId;
  }

  private String updateSecret(@NonNull String accountId, @NonNull EncryptedData encryptedData, @NonNull char[] secret) {
    secretManager.updateSecretText(accountId, encryptedData.getUuid(),
            SecretText.builder()
                    .value(String.valueOf(secret))
                    .name(encryptedData.getName())
                    .scopedToAccount(encryptedData.isScopedToAccount())
                    .usageRestrictions(encryptedData.getUsageRestrictions())
                    .hideFromListing(true)
                    .build(),
            false);
    return encryptedData.getUuid();
  }

  private String createSecret(@NonNull String accountId, @NonNull char[] secret, @NonNull SettingVariableTypes type) {
    return secretManager.saveSecretText(accountId,
            (SecretText) SecretText.builder()
                    .value(String.valueOf(secret))
                    .name(UUID.randomUUID().toString())
                    .hideFromListing(true)
                    .build(),
            false);
  }

  private <T extends PersistentEntity> void updateEncryptionReferencesIfNecessary(
          @NonNull T entity, @NonNull String parentId, T savedEntity) {
    Optional<EncryptableSetting> encryptableSettingOptional = getEncryptableSetting(entity);
    Optional<EncryptableSetting> savedEncryptableSettingOptional =
            savedEntity != null ? getEncryptableSetting(savedEntity) : Optional.empty();

    try {
      Map<String, Set<EncryptedDataParent>> currentState = encryptableSettingOptional.isPresent()
              ? buildSecretIdsToParentsMap(encryptableSettingOptional.get(), parentId)
              : new HashMap<>();
      Map<String, Set<EncryptedDataParent>> previousState = savedEncryptableSettingOptional.isPresent()
              ? buildSecretIdsToParentsMap(savedEncryptableSettingOptional.get(), parentId)
              : new HashMap<>();

      if (!previousState.isEmpty() || !currentState.isEmpty()) {
        List<SecretParentsUpdateDetail> secretParentsUpdateDetails =
                fetchSecretParentsUpdateDetailList(previousState, currentState);

        for (SecretParentsUpdateDetail secretParentsUpdateDetail : secretParentsUpdateDetails) {
          updateParent(secretParentsUpdateDetail);
        }
      }

    } catch (IllegalAccessException e) {
      throw new EncryptDecryptException(
              String.format(
                      "Illegal access exception while accessing the encrypted fields of object of Class %s while updating parents",
                      entity.getClass()),
              e);
    }
  }

  private <T extends PersistentEntity> void deleteEncryptionReferenceIfNecessary(T entity) {
    Optional<T> savedEntityOptional = getSavedEntity(entity);
    try {
      if (savedEntityOptional.isPresent()) {
        String parentId = ((UuidAware) savedEntityOptional.get()).getUuid();
        Optional<EncryptableSetting> encryptableSettingOptional = getEncryptableSetting(savedEntityOptional.get());
        Map<String, Set<EncryptedDataParent>> secretIdsToParentsMap = encryptableSettingOptional.isPresent()
                ? buildSecretIdsToParentsMap(encryptableSettingOptional.get(), parentId)
                : new HashMap<>();
        if (secretIdsToParentsMap.size() > 0) {
          List<SecretParentsUpdateDetail> secretParentsUpdateDetails =
                  fetchSecretParentsUpdateDetailList(secretIdsToParentsMap, new HashMap<>());
          for (SecretParentsUpdateDetail secretParentsUpdateDetail : secretParentsUpdateDetails) {
            updateParent(secretParentsUpdateDetail);
          }
        }
      }
    } catch (IllegalAccessException e) {
      throw new EncryptDecryptException(
              String.format(
                      "Illegal access exception while accessing the encrypted fields of object of Class %s while removing parents",
                      entity.getClass()),
              e);
    }
  }

  private void updateParent(@NonNull SecretParentsUpdateDetail secretParentsUpdateDetail) {
    EncryptedData encryptedData =
            Optional.ofNullable(get(EncryptedData.class, secretParentsUpdateDetail.getSecretId())).orElse(null);

    if (encryptedData == null) {
      return;
    }

    for (EncryptedDataParent encryptedDataParent : secretParentsUpdateDetail.getParentsToAdd()) {
      encryptedData.addParent(encryptedDataParent);
    }
    for (EncryptedDataParent encryptedDataParent : secretParentsUpdateDetail.getParentsToRemove()) {
      encryptedData.removeParent(encryptedDataParent);
    }

    save(encryptedData);
  }

  private <T extends PersistentEntity> Query<T> authorizeQuery(Class<T> collectionClass, Query query) {
    if (authFilters(query, collectionClass)) {
      return query;
    }
    User user = UserThreadLocal.get();
    if (user != null) {
      log.error("User [{}] doesn't have enough permission to perform operation. ", user);
    }
    throw new WingsException(ErrorCode.ACCESS_DENIED, USER);
  }
  private <T> boolean authFilters(Query query, Class<T> beanClass) {
    User user = UserThreadLocal.get();
    // If its not a user operation, return
    if (user == null) {
      return true;
    }

    UserRequestContext userRequestContext = user.getUserRequestContext();

    // No user request context set by the filter.
    if (userRequestContext == null) {
      return true;
    }

    if (userRequestContext.isAppIdFilterRequired()) {
      if (CollectionUtils.isNotEmpty(userRequestContext.getAppIds())) {
        UserPermissionInfo userPermissionInfo = userRequestContext.getUserPermissionInfo();
        if (AccountAccess.class.isAssignableFrom(beanClass) && userPermissionInfo.isHasAllAppAccess()) {
          query.field("accountId").equal(userRequestContext.getAccountId());
        } else {
          query.field("appId").in(userRequestContext.getAppIds());
        }
      } else {
        return false;
      }
    }

    if (userRequestContext.isEntityIdFilterRequired()) {
      String beanClassName = beanClass.getName();

      UserRequestContext.EntityInfo entityInfo = userRequestContext.getEntityInfoMap().get(beanClassName);

      if (entityInfo == null) {
        return true;
      }

      if (CollectionUtils.isNotEmpty(entityInfo.getEntityIds())) {
        query.field(entityInfo.getEntityFieldName()).in(entityInfo.getEntityIds());
      } else {
        return false;
      }
    }

    return true;
  }

}
