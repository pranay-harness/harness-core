package software.wings.dl;

import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.encryption.EncryptionReflectUtils.getDecryptedField;
import static io.harness.encryption.EncryptionReflectUtils.getEncryptedRefField;
import static io.harness.exception.WingsException.USER;
import static io.harness.persistence.HQuery.allChecks;
import static io.harness.persistence.ReadPref.NORMAL;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.utils.WingsReflectionUtils.isSetByYaml;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.dropwizard.lifecycle.Managed;
import io.harness.beans.EmbeddedUser;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.encryption.Encrypted;
import io.harness.exception.WingsException;
import io.harness.mongo.MongoPersistence;
import io.harness.mongo.PageController;
import io.harness.persistence.HIterator;
import io.harness.persistence.HQuery;
import io.harness.persistence.HQuery.QueryChecks;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.ReadPref;
import io.harness.persistence.UuidAware;
import io.harness.reflection.ReflectionUtils;
import io.harness.security.encryption.EncryptionType;
import org.apache.commons.collections.CollectionUtils;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.DatastoreImpl;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.Base;
import software.wings.beans.ServiceVariable;
import software.wings.beans.ServiceVariable.Type;
import software.wings.beans.SettingAttribute;
import software.wings.beans.User;
import software.wings.security.UserRequestContext;
import software.wings.security.UserRequestContext.EntityInfo;
import software.wings.security.UserThreadLocal;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.SecretChangeLog;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * The Class WingsMongoPersistence.
 */
@Singleton
public class WingsMongoPersistence extends MongoPersistence implements WingsPersistence, Managed {
  @Inject private SecretManager secretManager;

  /**
   * Creates a new object for wings mongo persistence.
   *
   * @param primaryDatastore   primary datastore for critical reads and writes.
   * @param secondaryDatastore replica of primary for non critical reads.
   */
  @Inject
  public WingsMongoPersistence(@Named("primaryDatastore") AdvancedDatastore primaryDatastore,
      @Named("secondaryDatastore") AdvancedDatastore secondaryDatastore) {
    super(primaryDatastore, secondaryDatastore);
  }

  @Override
  public <T extends PersistentEntity> T getWithAppId(Class<T> cls, String appId, String id) {
    return getWithAppId(cls, appId, id, NORMAL);
  }

  @Override
  public <T extends PersistentEntity> T getWithAppId(Class<T> cls, String appId, String id, ReadPref readPref) {
    return createQuery(cls, readPref).filter("appId", appId).filter(ID_KEY, id).get();
  }

  @Override
  public <T extends PersistentEntity> String save(T object) {
    encryptIfNecessary(object);
    String key = super.save(object);
    updateParentIfNecessary(object, key);
    return key;
  }

  @Override
  public <T extends PersistentEntity> void saveIgnoringDuplicateKeys(List<T> ts) {
    for (Iterator<T> iterator = ts.iterator(); iterator.hasNext();) {
      T t = iterator.next();
      if (t != null) {
        this.encryptIfNecessary(t);
      }
    }

    super.saveIgnoringDuplicateKeys(ts);
  }

  @Override
  public <T extends Base> T saveAndGet(Class<T> cls, T object) {
    Object id = save(object);
    Query<T> query = createQuery(cls, ReadPref.CRITICAL).filter(ID_KEY, id);
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
    final AdvancedDatastore datastore = getDatastore(cls, ReadPref.NORMAL);

    Query<T> query = datastore.createQuery(cls).filter(ID_KEY, entityId);
    UpdateOperations<T> operations = datastore.createUpdateOperations(cls);
    boolean encryptable = EncryptableSetting.class.isAssignableFrom(cls);
    Object savedObject = datastore.get(cls, entityId);
    List<Field> declaredAndInheritedFields = ReflectionUtils.getAllDeclaredAndInheritedFields(cls);
    for (Entry<String, Object> entry : keyValuePairs.entrySet()) {
      Object value = entry.getValue();
      if (cls == SettingAttribute.class && entry.getKey().equalsIgnoreCase("value")
          && EncryptableSetting.class.isInstance(value)) {
        EncryptableSetting e = (EncryptableSetting) value;
        encrypt(e, (EncryptableSetting) ((SettingAttribute) savedObject).getValue());
        updateParentIfNecessary(savedObject, entityId);
        value = e;
      } else if (encryptable) {
        Field f = declaredAndInheritedFields.stream()
                      .filter(field -> field.getName().equals(entry.getKey()))
                      .findFirst()
                      .orElse(null);
        if (f == null) {
          throw new WingsException("Field " + entry.getKey() + " not found for update on class " + cls.getName());
        }
        if (f.getAnnotation(Encrypted.class) != null) {
          try {
            EncryptableSetting object = (EncryptableSetting) savedObject;

            if (shouldEncryptWhileUpdating(f, object, keyValuePairs, entityId)) {
              String accountId = object.getAccountId();
              Field encryptedField = getEncryptedRefField(f, object);
              String encryptedId = encrypt(object, (char[]) value, encryptedField, null);
              updateParentIfNecessary(object, entityId);
              operations.set(encryptedField.getName(), encryptedId);
              operations.unset(f.getName());
              continue;
            }
          } catch (IllegalAccessException ex) {
            throw new WingsException("Failed to encrypt secret", ex);
          }
        }
      }
      operations.set(entry.getKey(), value);
    }

    fieldsToRemove.forEach(fieldToRemove -> {
      if (encryptable) {
        EncryptableSetting object = (EncryptableSetting) savedObject;
        String accountId = object.getAccountId();
        Field f = ReflectionUtils.getFieldByName(savedObject.getClass(), fieldToRemove);
        Preconditions.checkNotNull(f, "Can't find " + fieldToRemove + " in class " + cls);
        if (f.getAnnotation(Encrypted.class) != null) {
          try {
            Field encryptedField = getEncryptedRefField(f, object);
            String encryptedId = encrypt(object, null, encryptedField, object);
            updateParentIfNecessary(object, entityId);
            operations.set(encryptedField.getName(), encryptedId);
          } catch (IllegalAccessException e) {
            throw new WingsException(
                "Failed to update record for " + fieldToRemove + " in " + cls + " id: " + entityId, e);
          }
        }
      }
      operations.unset(fieldToRemove);
    });

    update(query, operations);
  }

  private boolean shouldEncryptWhileUpdating(Field f, EncryptableSetting object, Map<String, Object> keyValuePairs,
      String entityId) throws IllegalAccessException {
    List<Field> encryptedFields = object.getEncryptedFields();
    if (object.getClass().equals(ServiceVariable.class)) {
      deleteEncryptionReference(object, Collections.singleton(f.getName()), entityId);
      if (keyValuePairs.get("type") == Type.ENCRYPTED_TEXT) {
        return true;
      }
      return false;
    }
    return encryptedFields.contains(f);
  }

  @Override
  public <T extends PersistentEntity> boolean delete(Class<T> cls, String uuid) {
    if (cls.equals(SettingAttribute.class) || EncryptableSetting.class.isAssignableFrom(cls)) {
      final AdvancedDatastore datastore = getDatastore(cls, ReadPref.NORMAL);
      Query<T> query = datastore.createQuery(cls).filter(ID_KEY, uuid);
      return delete(query);
    }

    return super.delete(cls, uuid);
  }

  @Override
  public <T extends PersistentEntity> boolean delete(String accountId, Class<T> cls, String uuid) {
    Query<T> query = getDatastore(DEFAULT_STORE, ReadPref.NORMAL)
                         .createQuery(cls)
                         .filter(ID_KEY, uuid)
                         .filter("accountId", accountId);
    return delete(query);
  }

  @Override
  public <T extends PersistentEntity> boolean delete(Class<T> cls, String appId, String uuid) {
    Query<T> query = getDatastore(cls, ReadPref.NORMAL).createQuery(cls).filter(ID_KEY, uuid).filter("appId", appId);
    return delete(query);
  }

  @Override
  public <T extends PersistentEntity> boolean delete(Query<T> query) {
    if (query.getEntityClass().equals(SettingAttribute.class)
        || EncryptableSetting.class.isAssignableFrom(query.getEntityClass())) {
      try (HIterator<T> records = new HIterator<>(query.fetch())) {
        while (records.hasNext()) {
          deleteEncryptionReferenceIfNecessary(records.next());
        }
      }
    }

    return super.delete(query);
  }

  @Override
  public <T extends PersistentEntity> boolean delete(T entity) {
    if (SettingAttribute.class.isInstance(entity) || EncryptableSetting.class.isInstance(entity)) {
      deleteEncryptionReferenceIfNecessary(entity);
    }
    return super.delete(entity);
  }

  @Override
  public <T> PageResponse<T> query(Class<T> cls, PageRequest<T> req) {
    return query(cls, req, allChecks);
  }

  @Override
  public <T> PageResponse<T> query(Class<T> cls, PageRequest<T> req, Set<QueryChecks> queryChecks) {
    if (!authFilters(req, cls)) {
      return aPageResponse().withTotal(0).build();
    }
    AdvancedDatastore advancedDatastore = getDatastore(cls, req.getReadPref());
    Query<T> query = advancedDatastore.createQuery(cls);

    ((HQuery) query).setQueryChecks(queryChecks);
    Mapper mapper = ((DatastoreImpl) advancedDatastore).getMapper();

    return PageController.queryPageRequest(advancedDatastore, query, mapper, cls, req);
  }

  @Override
  public void start() throws Exception {
    // Do nothing
  }

  @Override
  public void stop() throws Exception {
    close();
  }

  private <T> boolean authFilters(PageRequest<T> pageRequest, Class<T> beanClass) {
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
        pageRequest.addFilter("appId", Operator.IN, userRequestContext.getAppIds().toArray());
      } else {
        return false;
      }
    }

    if (userRequestContext.isEntityIdFilterRequired()) {
      String beanClassName = beanClass.getName();

      EntityInfo entityInfo = userRequestContext.getEntityInfoMap().get(beanClassName);

      if (entityInfo == null) {
        return true;
      }

      if (CollectionUtils.isNotEmpty(entityInfo.getEntityIds())) {
        pageRequest.addFilter(entityInfo.getEntityFieldName(), Operator.IN, entityInfo.getEntityIds().toArray());
      } else {
        return false;
      }
    }

    return true;
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
        query.field("appId").in(userRequestContext.getAppIds());
      }
    }

    if (userRequestContext.isEntityIdFilterRequired()) {
      String beanClassName = beanClass.getName();

      EntityInfo entityInfo = userRequestContext.getEntityInfoMap().get(beanClassName);

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

  @Override
  public <T extends PersistentEntity> Query<T> createAuthorizedQuery(Class<T> collectionClass) {
    Query query = createQuery(collectionClass);
    if (authFilters(query, collectionClass)) {
      return query;
    }
    throw new WingsException(getExceptionMsgWithUserContext(), USER);
  }

  @Override
  public Query createAuthorizedQuery(Class collectionClass, boolean disableValidation) {
    Query query = createQuery(collectionClass);
    if (disableValidation) {
      query.disableValidation();
    }
    if (authFilters(query, collectionClass)) {
      return query;
    }
    throw new WingsException(getExceptionMsgWithUserContext(), USER);
  }

  private String getExceptionMsgWithUserContext() throws WingsException {
    User user = UserThreadLocal.get();
    StringBuilder buffer = new StringBuilder(
        "AuthFilter could not be applied since the user is not assigned to any apps / no app exists in the account.");
    if (user != null) {
      buffer.append(" User uuid: ");
      buffer.append(user.getUuid());
    }
    return buffer.toString();
  }

  /**
   * Encrypt an EncryptableSetting object. Currently assumes SimpleEncryption.
   *
   * @param object the object to be encrypted
   */
  private void encrypt(EncryptableSetting object, EncryptableSetting savedObject) {
    try {
      List<Field> fieldsToEncrypt = object.getEncryptedFields();
      for (Field f : fieldsToEncrypt) {
        f.setAccessible(true);
        char[] secret = (char[]) f.get(object);
        Field encryptedField = getEncryptedRefField(f, object);
        encryptedField.setAccessible(true);
        encrypt(object, secret, encryptedField, savedObject);
        f.set(object, null);
      }
    } catch (SecurityException e) {
      throw new WingsException("Security exception in encrypt", e);
    } catch (IllegalAccessException e) {
      throw new WingsException("Illegal access exception in encrypt", e);
    }
  }

  private String encrypt(EncryptableSetting object, char[] secret, Field encryptedField, EncryptableSetting savedObject)
      throws IllegalAccessException {
    encryptedField.setAccessible(true);
    Field decryptedField = getDecryptedField(encryptedField, object);
    decryptedField.setAccessible(true);

    // yaml ref case
    if (isSetByYaml(object, encryptedField)) {
      EncryptedData encryptedData = secretManager.getEncryptedDataFromYamlRef((String) encryptedField.get(object));
      encryptedField.set(object, encryptedData.getUuid());
      return encryptedData.getUuid();
    }

    if (isReferencedSecretText(object, encryptedField)) {
      encryptedField.set(object, String.valueOf(secret));
      return String.valueOf(secret);
    }

    final String accountId = object.getAccountId();
    EncryptionType encryptionType = secretManager.getEncryptionType(accountId);
    String encryptedId =
        savedObject == null ? (String) encryptedField.get(object) : (String) encryptedField.get(savedObject);
    EncryptedData encryptedData = isBlank(encryptedId) ? null : get(EncryptedData.class, encryptedId);
    String path = encryptedData == null ? null : encryptedData.getPath();
    EncryptedData encryptedPair = secretManager.encrypt(encryptionType, accountId, object.getSettingType(), secret,
        path, encryptedData, UUID.randomUUID().toString(), null);

    String changeLogDescription = "";

    if (encryptedData == null) {
      encryptedData = encryptedPair;
      encryptedData.setAccountId(accountId);
      encryptedData.setType(object.getSettingType());
      changeLogDescription = "Created";
    } else {
      encryptedData.setEncryptionKey(encryptedPair.getEncryptionKey());
      encryptedData.setEncryptedValue(encryptedPair.getEncryptedValue());
      encryptedData.setEncryptionType(encryptedPair.getEncryptionType());
      encryptedData.setKmsId(encryptedPair.getKmsId());
      changeLogDescription = "Changed " + decryptedField.getName();
    }

    encryptedId = save(encryptedData);
    if (UserThreadLocal.get() != null) {
      save(SecretChangeLog.builder()
               .accountId(accountId)
               .encryptedDataId(encryptedId)
               .description(changeLogDescription)
               .user(EmbeddedUser.builder()
                         .uuid(UserThreadLocal.get().getUuid())
                         .email(UserThreadLocal.get().getEmail())
                         .name(UserThreadLocal.get().getName())
                         .build())
               .build());
    }
    encryptedField.set(object, encryptedId);
    return encryptedId;
  }

  private boolean isReferencedSecretText(EncryptableSetting object, Field encryptedField) {
    if (!ServiceVariable.class.isInstance(object)) {
      return false;
    }
    ServiceVariable serviceVariable = (ServiceVariable) object;
    if (isNotBlank(serviceVariable.getUuid())) {
      Field decryptedField = getDecryptedField(encryptedField, object);
      deleteEncryptionReference(object, Sets.newHashSet(decryptedField.getName()), serviceVariable.getUuid());
    }
    return true;
  }

  private void updateParent(EncryptableSetting object, String parentId) {
    List<Field> fieldsToEncrypt = object.getEncryptedFields();
    for (Field f : fieldsToEncrypt) {
      f.setAccessible(true);
      Field encryptedField = getEncryptedRefField(f, object);
      encryptedField.setAccessible(true);
      String encryptedId;
      try {
        encryptedId = (String) encryptedField.get(object);
      } catch (IllegalAccessException e) {
        throw new WingsException("Error updating parent for encrypted record", e);
      }

      if (isBlank(encryptedId)) {
        continue;
      }

      EncryptedData encryptedData = get(EncryptedData.class, encryptedId);
      if (encryptedData == null) {
        continue;
      }

      if (encryptedData.getParentIds() == null || !encryptedData.getParentIds().contains(parentId)) {
        encryptedData.addParent(parentId);
        save(encryptedData);
      }
    }
  }

  @Override
  public <T extends Base> List<T> getAllEntities(PageRequest<T> pageRequest, Callable<PageResponse<T>> callable) {
    List<T> result = Lists.newArrayList();
    long currentPageSize;
    long total;
    long countSoFar = 0;

    try {
      do {
        pageRequest.setOffset(Long.toString(countSoFar));
        PageResponse<T> pageResponse = callable.call();
        total = pageResponse.getTotal();
        List<T> listFromResponse = pageResponse.getResponse();
        if (isEmpty(listFromResponse)) {
          break;
        }
        currentPageSize = listFromResponse.size();
        countSoFar += currentPageSize;
        result.addAll(listFromResponse);
      } while (countSoFar < total);

    } catch (Exception e) {
      throw new WingsException(e);
    }
    return result;
  }

  private void deleteEncryptionReference(EncryptableSetting object, Set<String> fieldNames, String parentId) {
    List<Field> fieldsToEncrypt = object.getEncryptedFields();
    for (Field f : fieldsToEncrypt) {
      if (fieldNames != null && !fieldNames.contains(f.getName())) {
        continue;
      }

      Field encryptedField = getEncryptedRefField(f, object);
      encryptedField.setAccessible(true);
      String encryptedId;
      try {
        encryptedId = (String) encryptedField.get(object);
      } catch (IllegalAccessException e) {
        throw new WingsException("Could not delete referenced record", e);
      }

      if (isBlank(encryptedId)) {
        continue;
      }

      EncryptedData encryptedData = get(EncryptedData.class, encryptedId);
      if (encryptedData == null) {
        continue;
      }
      encryptedData.removeParentId(parentId);
      if (isEmpty(encryptedData.getParentIds()) && encryptedData.getType() != SettingVariableTypes.SECRET_TEXT) {
        delete(encryptedData);
      } else {
        save(encryptedData);
      }
    }
  }

  private <T extends PersistentEntity> void encryptIfNecessary(T entity) {
    // if its an update
    Object savedObject = null;
    if (entity instanceof UuidAware) {
      String uuid = ((UuidAware) entity).getUuid();
      if (isNotBlank(uuid)
          && (SettingAttribute.class.isInstance(entity) || EncryptableSetting.class.isInstance(entity))) {
        savedObject = getDatastore(entity, ReadPref.NORMAL).get((Class<T>) entity.getClass(), uuid);
      }
    }
    Object toEncrypt = entity;
    if (SettingAttribute.class.isInstance(entity)) {
      toEncrypt = ((SettingAttribute) entity).getValue();
      savedObject = savedObject == null ? null : ((SettingAttribute) savedObject).getValue();
    }

    if (EncryptableSetting.class.isInstance(toEncrypt)) {
      encrypt((EncryptableSetting) toEncrypt, (EncryptableSetting) savedObject);
    }
  }

  private void updateParentIfNecessary(Object o, String parentId) {
    if (SettingAttribute.class.isInstance(o)) {
      o = ((SettingAttribute) o).getValue();
    }

    if (EncryptableSetting.class.isInstance(o)) {
      updateParent((EncryptableSetting) o, parentId);
    }
  }

  private <T extends PersistentEntity> void deleteEncryptionReferenceIfNecessary(T entity) {
    if (!(entity instanceof UuidAware)) {
      return;
    }
    String uuid = ((UuidAware) entity).getUuid();
    if (isBlank(uuid)) {
      return;
    }

    Object toDelete = getDatastore(entity, ReadPref.NORMAL).get(entity.getClass(), uuid);
    if (toDelete == null) {
      return;
    }
    if (SettingAttribute.class.isInstance(toDelete)) {
      toDelete = ((SettingAttribute) toDelete).getValue();
    }

    if (EncryptableSetting.class.isInstance(toDelete)) {
      deleteEncryptionReference((EncryptableSetting) toDelete, null, uuid);
    }
  }
}
