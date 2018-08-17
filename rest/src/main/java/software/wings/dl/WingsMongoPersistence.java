package software.wings.dl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.lang.System.currentTimeMillis;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.dl.HQuery.allChecks;
import static software.wings.dl.PageResponse.PageResponseBuilder.aPageResponse;
import static software.wings.exception.WingsException.USER;
import static software.wings.utils.WingsReflectionUtils.getDeclaredAndInheritedFields;
import static software.wings.utils.WingsReflectionUtils.getDecryptedField;
import static software.wings.utils.WingsReflectionUtils.getEncryptedRefField;
import static software.wings.utils.WingsReflectionUtils.getFieldByName;
import static software.wings.utils.WingsReflectionUtils.isSetByYaml;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import com.mongodb.DBCollection;
import com.mongodb.DuplicateKeyException;
import com.mongodb.WriteResult;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import io.dropwizard.lifecycle.Managed;
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
import software.wings.annotation.Encryptable;
import software.wings.annotation.Encrypted;
import software.wings.beans.Base;
import software.wings.beans.EmbeddedUser;
import software.wings.beans.ReadPref;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.ServiceVariable;
import software.wings.beans.ServiceVariable.Type;
import software.wings.beans.SettingAttribute;
import software.wings.beans.User;
import software.wings.dl.HQuery.QueryChecks;
import software.wings.exception.WingsException;
import software.wings.security.EncryptionType;
import software.wings.security.UserRequestContext;
import software.wings.security.UserRequestContext.EntityInfo;
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
public class WingsMongoPersistence implements WingsPersistence, Managed {
  protected static Logger logger = LoggerFactory.getLogger(WingsMongoPersistence.class);

  @Inject private SecretManager secretManager;
  private AdvancedDatastore primaryDatastore;
  private AdvancedDatastore secondaryDatastore;

  private Map<ReadPref, AdvancedDatastore> datastoreMap;

  /**
   * Creates a new object for wings mongo persistence.
   *
   * @param primaryDatastore   primary datastore for critical reads and writes.
   * @param secondaryDatastore replica of primary for non critical reads.
   * @param datastoreMap       datastore map based on read preference to datastore.
   */
  @Inject
  public WingsMongoPersistence(@Named("primaryDatastore") AdvancedDatastore primaryDatastore,
      @Named("secondaryDatastore") AdvancedDatastore secondaryDatastore,
      @Named("datastoreMap") Map<ReadPref, AdvancedDatastore> datastoreMap) {
    this.primaryDatastore = primaryDatastore;
    this.secondaryDatastore = secondaryDatastore;
    this.datastoreMap = datastoreMap;
  }

  @Override
  public <T extends Base> T get(Class<T> cls, String id) {
    return get(cls, id, ReadPref.NORMAL);
  }

  @Override
  public <T extends Base> T get(Class<T> cls, String appId, String id) {
    return get(cls, appId, id, ReadPref.NORMAL);
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
    Key<T> key = primaryDatastore.merge(t);
    return (String) key.getId();
  }

  @Override
  public <T extends Base> String save(T object) {
    encryptIfNecessary(object);
    Key<T> key = primaryDatastore.save(object);
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
      keys = primaryDatastore.insert(ts, insertOptions);
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
    return primaryDatastore.update(updateQuery, updateOperations);
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
    return primaryDatastore.findAndModify(query, updateOperations, new FindAndModifyOptions().upsert(true));
  }

  @Override
  public <T extends Base> T findAndModify(
      Query<T> query, UpdateOperations<T> updateOperations, FindAndModifyOptions findAndModifyOptions) {
    return primaryDatastore.findAndModify(query, updateOperations, findAndModifyOptions);
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
    return primaryDatastore.update(ent, ops);
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
    Query<T> query = primaryDatastore.createQuery(cls).filter(ID_KEY, entityId);
    UpdateOperations<T> operations = primaryDatastore.createUpdateOperations(cls);
    boolean encryptable = Encryptable.class.isAssignableFrom(cls);
    Object savedObject = datastoreMap.get(ReadPref.NORMAL).get(cls, entityId);
    List<Field> declaredAndInheritedFields = getDeclaredAndInheritedFields(cls);
    for (Entry<String, Object> entry : keyValuePairs.entrySet()) {
      Object value = entry.getValue();
      if (cls == SettingAttribute.class && entry.getKey().equalsIgnoreCase("value")
          && Encryptable.class.isInstance(value)) {
        Encryptable e = (Encryptable) value;
        encrypt(e, (Encryptable) ((SettingAttribute) savedObject).getValue());
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
            Encryptable object = (Encryptable) savedObject;

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
        Encryptable object = (Encryptable) savedObject;
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

  private boolean shouldEncryptWhileUpdating(
      Field f, Encryptable object, Map<String, Object> keyValuePairs, String entityId) throws IllegalAccessException {
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
    if (cls.equals(SettingAttribute.class) || Encryptable.class.isAssignableFrom(cls)) {
      Query<T> query = primaryDatastore.createQuery(cls).filter(ID_KEY, uuid);
      return delete(query);
    }
    WriteResult result = primaryDatastore.delete(cls, uuid);
    return !(result == null || result.getN() == 0);
  }

  @Override
  public <T extends Base> boolean delete(String accountId, Class<T> cls, String uuid) {
    Query<T> query = primaryDatastore.createQuery(cls).filter(ID_KEY, uuid).filter("accountId", accountId);
    return delete(query);
  }

  @Override
  public <T extends Base> boolean delete(Class<T> cls, String appId, String uuid) {
    Query<T> query = primaryDatastore.createQuery(cls).filter(ID_KEY, uuid).filter("appId", appId);
    return delete(query);
  }

  @Override
  public <T extends Base> boolean delete(Query<T> query) {
    if (query.getEntityClass().equals(SettingAttribute.class)
        || Encryptable.class.isAssignableFrom(query.getEntityClass())) {
      try (HIterator<T> records = new HIterator<>(query.fetch())) {
        while (records.hasNext()) {
          deleteEncryptionReferenceIfNecessary(records.next());
        }
      }
    }
    WriteResult result = primaryDatastore.delete(query);
    return !(result == null || result.getN() == 0);
  }

  @Override
  public <T extends Base> boolean delete(T object) {
    if (SettingAttribute.class.isInstance(object) || Encryptable.class.isInstance(object)) {
      deleteEncryptionReferenceIfNecessary(object);
    }
    WriteResult result = primaryDatastore.delete(object);
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
    ReadPref readPref = req.getReadPref() != null ? req.getReadPref() : ReadPref.NORMAL;
    AdvancedDatastore advancedDatastore = datastoreMap.get(readPref);
    Query<T> query = advancedDatastore.createQuery(cls);

    ((HQuery) query).setQueryChecks(queryChecks);
    Mapper mapper = ((DatastoreImpl) advancedDatastore).getMapper();

    return MongoHelper.queryPageRequest(advancedDatastore, query, mapper, cls, req);
  }

  @Override
  public <T> long getCount(Class<T> cls, PageRequest<T> req) {
    AdvancedDatastore advancedDatastore =
        datastoreMap.get(req.getReadPref() != null ? req.getReadPref() : ReadPref.NORMAL);
    Query<T> query = advancedDatastore.createQuery(cls);
    Mapper mapper = ((DatastoreImpl) advancedDatastore).getMapper();

    return MongoHelper.getCount(advancedDatastore, query, mapper, cls, req);
  }

  @Override
  public <T> UpdateOperations<T> createUpdateOperations(Class<T> cls) {
    return primaryDatastore.createUpdateOperations(cls);
  }

  @Override
  public <T> Query<T> createQuery(Class<T> cls) {
    return createQuery(cls, ReadPref.NORMAL);
  }

  @Override
  public <T> Query<T> createQuery(Class<T> cls, Set<QueryChecks> queryChecks) {
    Query<T> query = createQuery(cls, ReadPref.NORMAL);
    ((HQuery) query).setQueryChecks(queryChecks);
    return query;
  }

  @Override
  public <T> Query<T> createQuery(Class<T> cls, ReadPref readPref) {
    return datastoreMap.get(readPref).createQuery(cls);
  }

  @Override
  public GridFSBucket getOrCreateGridFSBucket(String bucketName) {
    return GridFSBuckets.create(
        primaryDatastore.getMongo().getDatabase(primaryDatastore.getDB().getName()), bucketName);
  }

  @Override
  public AdvancedDatastore getDatastore() {
    return primaryDatastore;
  }

  @Override
  public void close() {
    primaryDatastore.getMongo().close();
  }

  @Override
  public DBCollection getCollection(String collectionName) {
    return primaryDatastore.getDB().getCollection(collectionName);
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
  public <T> Query<T> createAuthorizedQuery(Class<T> collectionClass) {
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
   * Encrypt an Encryptable object. Currently assumes SimpleEncryption.
   *
   * @param object the object to be encrypted
   */
  private void encrypt(Encryptable object, Encryptable savedObject) {
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

  private String encrypt(Encryptable object, char[] secret, Field encryptedField, Encryptable savedObject)
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

  private boolean isReferencedSecretText(Encryptable object, Field encryptedField) {
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

  private void updateParent(Encryptable object, String parentId) {
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

  private void deleteEncryptionReference(Encryptable object, Set<String> fieldNames, String parentId) {
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
    if (isNotBlank(o.getUuid()) && (SettingAttribute.class.isInstance(o) || Encryptable.class.isInstance(o))) {
      savedObject = datastoreMap.get(ReadPref.NORMAL).get((Class<T>) o.getClass(), o.getUuid());
    }
    Object toEncrypt = o;
    if (SettingAttribute.class.isInstance(o)) {
      toEncrypt = ((SettingAttribute) o).getValue();
      savedObject = savedObject == null ? null : ((SettingAttribute) savedObject).getValue();
    }

    if (Encryptable.class.isInstance(toEncrypt)) {
      encrypt((Encryptable) toEncrypt, (Encryptable) savedObject);
    }
  }

  private void updateParentIfNecessary(Object o, String parentId) {
    if (SettingAttribute.class.isInstance(o)) {
      o = ((SettingAttribute) o).getValue();
    }

    if (Encryptable.class.isInstance(o)) {
      updateParent((Encryptable) o, parentId);
    }
  }

  private <T extends Base> void deleteEncryptionReferenceIfNecessary(T o) {
    if (isBlank(o.getUuid())) {
      return;
    }

    Object toDelete = datastoreMap.get(ReadPref.NORMAL).get(o.getClass(), o.getUuid());
    if (toDelete == null) {
      return;
    }
    if (SettingAttribute.class.isInstance(toDelete)) {
      toDelete = ((SettingAttribute) toDelete).getValue();
    }

    if (Encryptable.class.isInstance(toDelete)) {
      deleteEncryptionReference((Encryptable) toDelete, null, o.getUuid());
    }
  }
}
