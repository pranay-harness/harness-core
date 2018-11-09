package software.wings.dl;

import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.exception.WingsException.USER;
import static io.harness.persistence.HQuery.allChecks;
import static io.harness.persistence.ReadPref.NORMAL;
import static io.harness.reflection.ReflectUtils.getDeclaredAndInheritedFields;
import static io.harness.reflection.ReflectUtils.getDecryptedField;
import static io.harness.reflection.ReflectUtils.getEncryptedRefField;
import static io.harness.reflection.ReflectUtils.getFieldByName;
import static java.lang.System.currentTimeMillis;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.utils.WingsReflectionUtils.isSetByYaml;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import com.mongodb.DuplicateKeyException;
import com.mongodb.WriteResult;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import io.dropwizard.lifecycle.Managed;
import io.harness.annotation.Encrypted;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.exception.WingsException;
import io.harness.mongo.MongoPersistence;
import io.harness.mongo.MongoUtils;
import io.harness.mongo.PageController;
import io.harness.persistence.HIterator;
import io.harness.persistence.HQuery;
import io.harness.persistence.HQuery.QueryChecks;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.ReadPref;
import org.apache.commons.collections.CollectionUtils;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.DatastoreImpl;
import org.mongodb.morphia.FindAndModifyOptions;
import org.mongodb.morphia.InsertOptions;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.Base;
import software.wings.beans.EmbeddedUser;
import software.wings.beans.ServiceVariable;
import software.wings.beans.ServiceVariable.Type;
import software.wings.beans.SettingAttribute;
import software.wings.beans.User;
import software.wings.security.EncryptionType;
import software.wings.security.UserRequestContext;
import software.wings.security.UserRequestContext.EntityInfo;
import software.wings.security.UserRequestInfo;
import software.wings.security.UserThreadLocal;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.SecretChangeLog;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * The Class WingsMongoPersistence.
 */
@Singleton
public class WingsMongoPersistence extends MongoPersistence implements WingsPersistence, Managed {
  protected static Logger logger = LoggerFactory.getLogger(WingsMongoPersistence.class);

  @Inject private SecretManager secretManager;

  /**
   * Creates a new object for wings mongo persistence.
   *
   * @param primaryDatastore   primary datastore for critical reads and writes.
   * @param secondaryDatastore replica of primary for non critical reads.
   * @param datastoreMap       datastore map based on read preference to datastore.
   */
  @Inject
  public WingsMongoPersistence(@Named("primaryDatastore") AdvancedDatastore primaryDatastore,
      @Named("secondaryDatastore") AdvancedDatastore secondaryDatastore) {
    super(primaryDatastore, secondaryDatastore);
  }

  @Override
  public <T extends Base> T get(Class<T> cls, String id) {
    return get(cls, id, NORMAL);
  }

  @Override
  public <T extends Base> T get(Class<T> cls, String appId, String id) {
    return get(cls, appId, id, NORMAL);
  }

  @Override
  public <T extends Base> T get(Class<T> cls, String appId, String id, ReadPref readPref) {
    return createQuery(cls, readPref).filter("appId", appId).filter(ID_KEY, id).get();
  }

  @Override
  public <T extends Base> T get(Class<T> cls, String id, ReadPref readPref) {
    return createQuery(cls, readPref).filter(ID_KEY, id).get();
  }

  @Override
  public <T extends Base> T get(Class<T> cls, PageRequest<T> req) {
    req.setLimit("1");
    PageResponse<T> res = query(cls, req);
    if (isEmpty(res)) {
      return null;
    }
    return res.get(0);
  }

  @Override
  public <T extends Base> String merge(T t) {
    Key<T> key = getDatastore(DEFAULT_STORE, ReadPref.NORMAL).merge(t);
    return (String) key.getId();
  }

  @Override
  public <T extends Base> String save(T object) {
    encryptIfNecessary(object);
    Key<T> key = getDatastore(DEFAULT_STORE, ReadPref.NORMAL).save(object);
    updateParentIfNecessary(object, (String) key.getId());
    return (String) key.getId();
  }

  @Override
  public <T extends Base> List<String> save(List<T> ts) {
    ts.removeIf(Objects::isNull);
    List<String> ids = new ArrayList<>();
    for (T t : ts) {
      ids.add(save(t));
    }
    return ids;
  }

  @Override
  public <T extends Base> List<String> saveIgnoringDuplicateKeys(List<T> ts) {
    for (Iterator<T> iterator = ts.iterator(); iterator.hasNext();) {
      T t = iterator.next();
      if (t == null) {
        iterator.remove();
        continue;
      }
      this.encryptIfNecessary(t);
    }
    InsertOptions insertOptions = new InsertOptions();
    insertOptions.continueOnError(true);
    Iterable<Key<T>> keys = new ArrayList<>();
    try {
      keys = getDatastore(DEFAULT_STORE, ReadPref.NORMAL).insert(ts, insertOptions);
    } catch (DuplicateKeyException dke) {
      // ignore
    }
    List<String> ids = new ArrayList<>();
    keys.forEach(tKey -> ids.add((String) tKey.getId()));
    return ids;
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
  public <T> UpdateResults update(Query<T> updateQuery, UpdateOperations<T> updateOperations) {
    // TODO: add encryption handling; right now no encrypted classes use update
    // When necessary, we can fix this by adding Class<T> cls to the args and then similar to updateField
    updateOperations.set("lastUpdatedAt", currentTimeMillis());
    if (UserThreadLocal.get() != null) {
      updateOperations.set("lastUpdatedBy",
          EmbeddedUser.builder()
              .uuid(UserThreadLocal.get().getUuid())
              .email(UserThreadLocal.get().getEmail())
              .name(UserThreadLocal.get().getName())
              .build());
    }
    return getDatastore(DEFAULT_STORE, ReadPref.NORMAL).update(updateQuery, updateOperations);
  }

  @Override
  public <T> T upsert(Query<T> query, UpdateOperations<T> updateOperations) {
    // TODO: add encryption handling; right now no encrypted classes use upsert
    // When necessary, we can fix this by adding Class<T> cls to the args and then similar to updateField
    updateOperations.set("lastUpdatedAt", currentTimeMillis());
    if (UserThreadLocal.get() != null) {
      updateOperations.set("lastUpdatedBy",
          EmbeddedUser.builder()
              .uuid(UserThreadLocal.get().getUuid())
              .email(UserThreadLocal.get().getEmail())
              .name(UserThreadLocal.get().getName())
              .build());
      updateOperations.setOnInsert("createdBy",
          EmbeddedUser.builder()
              .uuid(UserThreadLocal.get().getUuid())
              .email(UserThreadLocal.get().getEmail())
              .name(UserThreadLocal.get().getName())
              .build());
    }
    updateOperations.setOnInsert("createdAt", currentTimeMillis());
    updateOperations.setOnInsert("_id", generateUuid());
    return getDatastore(DEFAULT_STORE, ReadPref.NORMAL)
        .findAndModify(query, updateOperations, new FindAndModifyOptions().upsert(true));
  }

  @Override
  public <T extends Base> T findAndModify(
      Query<T> query, UpdateOperations<T> updateOperations, FindAndModifyOptions findAndModifyOptions) {
    return getDatastore(DEFAULT_STORE, ReadPref.NORMAL).findAndModify(query, updateOperations, findAndModifyOptions);
  }

  @Override
  public <T extends Base> UpdateResults update(T ent, UpdateOperations<T> ops) {
    // TODO: add encryption handling; right now no encrypted classes use update
    // When necessary, we can fix this by adding Class<T> cls to the args and then similar to updateField
    ops.set("lastUpdatedAt", currentTimeMillis());
    if (UserThreadLocal.get() != null) {
      ops.set("lastUpdatedBy",
          EmbeddedUser.builder()
              .uuid(UserThreadLocal.get().getUuid())
              .email(UserThreadLocal.get().getEmail())
              .name(UserThreadLocal.get().getName())
              .build());
    }
    return getDatastore(DEFAULT_STORE, ReadPref.NORMAL).update(ent, ops);
  }

  @Override
  public <T> void updateField(Class<T> cls, String entityId, String fieldName, Object value) {
    Map<String, Object> keyValuePairs = new HashMap<>();
    keyValuePairs.put(fieldName, value);
    updateFields(cls, entityId, keyValuePairs);
  }

  @Override
  public <T> void updateFields(Class<T> cls, String entityId, Map<String, Object> keyValuePairs) {
    updateFields(cls, entityId, keyValuePairs, Collections.emptySet());
  }

  @Override
  public <T> void updateFields(
      Class<T> cls, String entityId, Map<String, Object> keyValuePairs, Set<String> fieldsToRemove) {
    final AdvancedDatastore datastore = getDatastore(DEFAULT_STORE, ReadPref.NORMAL);

    Query<T> query = datastore.createQuery(cls).filter(ID_KEY, entityId);
    UpdateOperations<T> operations = datastore.createUpdateOperations(cls);
    boolean encryptable = EncryptableSetting.class.isAssignableFrom(cls);
    Object savedObject = getDatastore(DEFAULT_STORE, ReadPref.NORMAL).get(cls, entityId);
    List<Field> declaredAndInheritedFields = getDeclaredAndInheritedFields(cls);
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
        Field f = getFieldByName(savedObject.getClass(), fieldToRemove);
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
  public <T extends Base> boolean delete(Class<T> cls, String uuid) {
    final AdvancedDatastore datastore = getDatastore(DEFAULT_STORE, ReadPref.NORMAL);
    if (cls.equals(SettingAttribute.class) || EncryptableSetting.class.isAssignableFrom(cls)) {
      Query<T> query = datastore.createQuery(cls).filter(ID_KEY, uuid);
      return delete(query);
    }
    WriteResult result = datastore.delete(cls, uuid);
    return !(result == null || result.getN() == 0);
  }

  @Override
  public <T extends Base> boolean delete(String accountId, Class<T> cls, String uuid) {
    Query<T> query = getDatastore(DEFAULT_STORE, ReadPref.NORMAL)
                         .createQuery(cls)
                         .filter(ID_KEY, uuid)
                         .filter("accountId", accountId);
    return delete(query);
  }

  @Override
  public <T extends Base> boolean delete(Class<T> cls, String appId, String uuid) {
    Query<T> query =
        getDatastore(DEFAULT_STORE, ReadPref.NORMAL).createQuery(cls).filter(ID_KEY, uuid).filter("appId", appId);
    return delete(query);
  }

  @Override
  public <T extends Base> boolean delete(Query<T> query) {
    if (query.getEntityClass().equals(SettingAttribute.class)
        || EncryptableSetting.class.isAssignableFrom(query.getEntityClass())) {
      try (HIterator<T> records = new HIterator<>(query.fetch())) {
        while (records.hasNext()) {
          deleteEncryptionReferenceIfNecessary(records.next());
        }
      }
    }
    WriteResult result = getDatastore(DEFAULT_STORE, ReadPref.NORMAL).delete(query);
    return !(result == null || result.getN() == 0);
  }

  @Override
  public <T extends Base> boolean delete(T object) {
    if (SettingAttribute.class.isInstance(object) || EncryptableSetting.class.isInstance(object)) {
      deleteEncryptionReferenceIfNecessary(object);
    }
    WriteResult result = getDatastore(DEFAULT_STORE, ReadPref.NORMAL).delete(object);
    return !(result == null || result.getN() == 0);
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
    AdvancedDatastore advancedDatastore = getDatastore(DEFAULT_STORE, req.getReadPref());
    Query<T> query = advancedDatastore.createQuery(cls);

    ((HQuery) query).setQueryChecks(queryChecks);
    Mapper mapper = ((DatastoreImpl) advancedDatastore).getMapper();

    return PageController.queryPageRequest(advancedDatastore, query, mapper, cls, req);
  }

  @Override
  public <T> long getCount(Class<T> cls, PageRequest<T> req) {
    AdvancedDatastore advancedDatastore = getDatastore(DEFAULT_STORE, req.getReadPref());
    Query<T> query = advancedDatastore.createQuery(cls);
    Mapper mapper = ((DatastoreImpl) advancedDatastore).getMapper();

    return MongoUtils.getCount(advancedDatastore, query, mapper, cls, req);
  }

  @Override
  public <T> UpdateOperations<T> createUpdateOperations(Class<T> cls) {
    return getDatastore(DEFAULT_STORE, ReadPref.NORMAL).createUpdateOperations(cls);
  }

  @Override
  public <T extends PersistentEntity> Query<T> createQuery(Class<T> cls) {
    return createQuery(cls, NORMAL);
  }

  @Override
  public <T extends PersistentEntity> Query<T> createQuery(Class<T> cls, Set<QueryChecks> queryChecks) {
    Query<T> query = createQuery(cls, NORMAL);
    ((HQuery) query).setQueryChecks(queryChecks);
    return query;
  }

  @Override
  public <T extends PersistentEntity> Query<T> createQuery(
      Class<T> cls, ReadPref readPref, Set<QueryChecks> queryChecks) {
    Query<T> query = createQuery(cls, readPref);
    ((HQuery) query).setQueryChecks(queryChecks);
    return query;
  }

  @Override
  public GridFSBucket getOrCreateGridFSBucket(String bucketName) {
    final AdvancedDatastore datastore = getDatastore(DEFAULT_STORE, ReadPref.NORMAL);
    return GridFSBuckets.create(datastore.getMongo().getDatabase(datastore.getDB().getName()), bucketName);
  }

  @Override
  public void close() {
    getDatastore(DEFAULT_STORE, ReadPref.NORMAL).getMongo().close();
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

    if (user.isUseNewRbac()) {
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

    } else {
      UserRequestInfo userRequestInfo = user.getUserRequestInfo();

      // No user request info set by the filter.
      if (userRequestInfo == null) {
        return true;
      }

      if (userRequestInfo.isAppIdFilterRequired()) {
        // TODO: field name should be dynamic
        boolean emptyAppIdsInUserReq = isEmpty(userRequestInfo.getAppIds());
        if (emptyAppIdsInUserReq) {
          if (isEmpty(userRequestInfo.getAllowedAppIds())) {
            return false;
          } else {
            pageRequest.addFilter("appId", Operator.IN, userRequestInfo.getAllowedAppIds().toArray());
          }
        } else {
          pageRequest.addFilter("appId", Operator.IN, userRequestInfo.getAppIds().toArray());
        }
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

    if (user.isUseNewRbac()) {
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

    } else {
      if (user.getUserRequestInfo() == null) {
        return true;
      }
      UserRequestInfo userRequestInfo = UserThreadLocal.get().getUserRequestInfo();
      if (userRequestInfo.isAppIdFilterRequired()) {
        // TODO: field name should be dynamic
        boolean emptyAppIdsInUserReq = isEmpty(userRequestInfo.getAppIds());

        if (emptyAppIdsInUserReq) {
          if (isEmpty(userRequestInfo.getAllowedAppIds())) {
            return false;
          } else {
            query.field("appId").in(userRequestInfo.getAllowedAppIds());
          }
        } else {
          query.field("appId").in(userRequestInfo.getAppIds());
        }
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
    EncryptedData encryptedPair = secretManager.encrypt(
        encryptionType, accountId, object.getSettingType(), secret, encryptedData, UUID.randomUUID().toString(), null);

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

  private <T extends Base> void encryptIfNecessary(T o) {
    // if its an update
    Object savedObject = null;
    if (isNotBlank(o.getUuid()) && (SettingAttribute.class.isInstance(o) || EncryptableSetting.class.isInstance(o))) {
      savedObject = getDatastore(DEFAULT_STORE, ReadPref.NORMAL).get((Class<T>) o.getClass(), o.getUuid());
    }
    Object toEncrypt = o;
    if (SettingAttribute.class.isInstance(o)) {
      toEncrypt = ((SettingAttribute) o).getValue();
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

  private <T extends Base> void deleteEncryptionReferenceIfNecessary(T o) {
    if (isBlank(o.getUuid())) {
      return;
    }

    Object toDelete = getDatastore(DEFAULT_STORE, ReadPref.NORMAL).get(o.getClass(), o.getUuid());
    if (toDelete == null) {
      return;
    }
    if (SettingAttribute.class.isInstance(toDelete)) {
      toDelete = ((SettingAttribute) toDelete).getValue();
    }

    if (EncryptableSetting.class.isInstance(toDelete)) {
      deleteEncryptionReference((EncryptableSetting) toDelete, null, o.getUuid());
    }
  }
}
