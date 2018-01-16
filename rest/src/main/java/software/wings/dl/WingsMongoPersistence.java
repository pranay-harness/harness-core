package software.wings.dl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.lang.System.currentTimeMillis;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.SearchFilter.Builder.aSearchFilter;
import static software.wings.utils.WingsReflectionUtils.getDeclaredAndInheritedFields;
import static software.wings.utils.WingsReflectionUtils.getDecryptedField;
import static software.wings.utils.WingsReflectionUtils.getEncryptedRefField;
import static software.wings.utils.WingsReflectionUtils.isSetByYaml;

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
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.FindAndModifyOptions;
import org.mongodb.morphia.InsertOptions;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
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
import software.wings.common.UUIDGenerator;
import software.wings.exception.WingsException;
import software.wings.security.EncryptionType;
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
public class WingsMongoPersistence implements WingsPersistence, Managed {
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

  /**
   * {@inheritDoc}
   */
  @Override
  public <T extends Base> List<T> list(Class<T> cls) {
    return list(cls, ReadPref.NORMAL);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T extends Base> List<T> list(Class<T> cls, ReadPref readPref) {
    return datastoreMap.get(readPref).find(cls).asList();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T extends Base> T get(Class<T> cls, String id) {
    return get(cls, id, ReadPref.NORMAL);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T extends Base> T get(Class<T> cls, String appId, String id) {
    return createQuery(cls).field("appId").equal(appId).field(ID_KEY).equal(id).get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T extends Base> T get(Class<T> cls, String appId, String id, ReadPref readPref) {
    return createQuery(cls, readPref).field("appId").equal(appId).field(ID_KEY).equal(id).get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T extends Base> T get(Class<T> cls, String id, ReadPref readPref) {
    return datastoreMap.get(readPref).get(cls, id);
  }

  @Override
  public <T extends Base> T executeGetOneQuery(Query<T> query) {
    return query.get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T extends Base> T get(Class<T> cls, PageRequest<T> req) {
    return get(cls, req, ReadPref.NORMAL);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T extends Base> T get(Class<T> cls, PageRequest<T> req, ReadPref readPref) {
    req.setLimit("1");
    PageResponse<T> res = query(cls, req, readPref);
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

  /**
   * {@inheritDoc}
   */
  @Override
  public <T extends Base> String save(T object) {
    encryptIfNecessary(object);
    Key<T> key = primaryDatastore.save(object);
    updateParentIfNecessary(object, (String) key.getId());
    return (String) key.getId();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T extends Base> List<String> save(List<T> ts) {
    ts.removeIf(Objects::isNull);
    List<String> ids = new ArrayList<>();
    for (T t : ts) {
      ids.add(save(t));
    }
    return ids;
  }

  /**
   * {@inheritDoc}
   */
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

  /**
   * {@inheritDoc}
   */
  @Override
  public <T extends Base> T saveAndGet(Class<T> cls, T object) {
    Object id = save(object);
    Query<T> query = createQuery(cls, ReadPref.CRITICAL).field(ID_KEY).equal(id);
    if (object.getShardKeys() != null) {
      object.getShardKeys().keySet().forEach(key -> query.field(key).equal(object.getShardKeys().get(key)));
    }
    return query.get();
  }

  /**
   * {@inheritDoc}
   *
   * @return
   */
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
    updateOperations.setOnInsert("_id", UUIDGenerator.getUuid());
    return primaryDatastore.findAndModify(query, updateOperations, new FindAndModifyOptions().upsert(true));
  }

  /**
   * {@inheritDoc}
   */
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

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> void updateField(Class<T> cls, String entityId, String fieldName, Object value) {
    Map<String, Object> keyValuePairs = new HashMap<>();
    keyValuePairs.put(fieldName, value);
    updateFields(cls, entityId, keyValuePairs);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> void updateFields(Class<T> cls, String entityId, Map<String, Object> keyValuePairs) {
    Query<T> query = primaryDatastore.createQuery(cls).field(ID_KEY).equal(entityId);
    UpdateOperations<T> operations = primaryDatastore.createUpdateOperations(cls);
    boolean encryptable = Encryptable.class.isAssignableFrom(cls);
    List<Field> declaredAndInheritedFields = getDeclaredAndInheritedFields(cls);
    for (Entry<String, Object> entry : keyValuePairs.entrySet()) {
      Object value = entry.getValue();
      if (cls == SettingAttribute.class && entry.getKey().equalsIgnoreCase("value")
          && Encryptable.class.isInstance(value)) {
        Encryptable e = (Encryptable) value;
        Object o = datastoreMap.get(ReadPref.NORMAL).get(cls, entityId);
        encrypt(e, (Encryptable) ((SettingAttribute) o).getValue());
        updateParentIfNecessary(o, entityId);
        value = e;
      } else if (encryptable) {
        Field f = declaredAndInheritedFields.stream()
                      .filter(field -> field.getName().equals(entry.getKey()))
                      .findFirst()
                      .orElse(null);
        if (f == null) {
          throw new WingsException("Field " + entry.getKey() + " not found for update on class " + cls.getName());
        }
        Encrypted a = f.getAnnotation(Encrypted.class);
        if (null != a) {
          try {
            Encryptable object = (Encryptable) datastoreMap.get(ReadPref.NORMAL).get(cls, entityId);

            if (shouldEncryptWhileUpdating(f, object, keyValuePairs, entityId)) {
              String accountId = object.getAccountId();
              Field encryptedField = getEncryptedRefField(f, object);
              String encryptedId =
                  encrypt(object, (char[]) value, encryptedField, null, secretManager.getEncryptionType(accountId));
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
  /**
   * {@inheritDoc}
   */
  @Override
  public <T extends Base> boolean delete(Class<T> cls, String uuid) {
    if (cls.equals(SettingAttribute.class) || Encryptable.class.isAssignableFrom(cls)) {
      Query<T> query = primaryDatastore.createQuery(cls).field(ID_KEY).equal(uuid);
      return delete(query);
    }
    WriteResult result = primaryDatastore.delete(cls, uuid);
    return !(result == null || result.getN() == 0);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T extends Base> boolean delete(Class<T> cls, String appId, String uuid) {
    Query<T> query = primaryDatastore.createQuery(cls).field(ID_KEY).equal(uuid).field("appId").equal(appId);
    return delete(query);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T extends Base> boolean delete(Query<T> query) {
    if (query.getEntityClass().equals(SettingAttribute.class)
        || Encryptable.class.isAssignableFrom(query.getEntityClass())) {
      List<T> objects = query.asList();
      for (T object : objects) {
        deleteEncryptionReferenceIfNecessary(object);
      }
    }
    WriteResult result = primaryDatastore.delete(query);
    return !(result == null || result.getN() == 0);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T extends Base> boolean delete(T object) {
    if (SettingAttribute.class.isInstance(object) || Encryptable.class.isInstance(object)) {
      deleteEncryptionReferenceIfNecessary(object);
    }
    WriteResult result = primaryDatastore.delete(object);
    return !(result == null || result.getN() == 0);
  }

  /**
   * {@inheritDoc}
   * It should be rarely used
   */
  @Override
  public <T> List<T> queryAll(Class<T> cls, PageRequest<T> req) {
    PageResponse<T> res = query(cls, req);
    List<T> ret = new ArrayList<>();
    while (isNotEmpty(res)) {
      ret.addAll(res.getResponse());
      req.setOffset(String.valueOf(ret.size()));
      res = query(cls, req);
    }
    return ret;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> PageResponse<T> query(Class<T> cls, PageRequest<T> req) {
    return query(cls, req, ReadPref.NORMAL);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> PageResponse<T> query(Class<T> cls, PageRequest<T> req, boolean disableValidation) {
    return query(cls, req, ReadPref.NORMAL, disableValidation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> PageResponse<T> query(Class<T> cls, PageRequest<T> req, ReadPref readPref) {
    if (!authFilters(req)) {
      return PageResponse.Builder.aPageResponse().withTotal(0).build();
    }
    if (readPref == ReadPref.NORMAL && req.getReadPref() == ReadPref.CRITICAL) {
      readPref = ReadPref.CRITICAL;
    }
    return MongoHelper.queryPageRequest(datastoreMap.get(readPref), cls, req, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> long getCount(Class<T> cls, PageRequest<T> req) {
    return MongoHelper.getCount(datastoreMap.get(ReadPref.NORMAL), cls, req);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> PageResponse<T> query(Class<T> cls, PageRequest<T> req, ReadPref readPref, boolean disableValidation) {
    if (!authFilters(req)) {
      return PageResponse.Builder.aPageResponse().withTotal(0).build();
    }
    return MongoHelper.queryPageRequest(datastoreMap.get(readPref), cls, req, disableValidation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> UpdateOperations<T> createUpdateOperations(Class<T> cls) {
    return primaryDatastore.createUpdateOperations(cls);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Query<T> createQuery(Class<T> cls) {
    return createQuery(cls, ReadPref.NORMAL);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Query<T> createQuery(Class<T> cls, ReadPref readPref) {
    return datastoreMap.get(readPref).createQuery(cls);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public GridFSBucket getOrCreateGridFSBucket(String bucketName) {
    return GridFSBuckets.create(
        primaryDatastore.getMongo().getDatabase(primaryDatastore.getDB().getName()), bucketName);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public AdvancedDatastore getDatastore() {
    return primaryDatastore;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() {
    primaryDatastore.getMongo().close();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DBCollection getCollection(String collectionName) {
    return primaryDatastore.getDB().getCollection(collectionName);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void start() throws Exception {
    // Do nothing
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void stop() throws Exception {
    close();
  }

  private boolean authFilters(PageRequest pageRequest) {
    if (UserThreadLocal.get() == null || UserThreadLocal.get().getUserRequestInfo() == null) {
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
          pageRequest.addFilter(
              aSearchFilter().withField("appId", Operator.IN, userRequestInfo.getAllowedAppIds().toArray()).build());
        }
      } else {
        pageRequest.addFilter(
            aSearchFilter().withField("appId", Operator.IN, userRequestInfo.getAppIds().toArray()).build());
      }
    } else if (userRequestInfo.isEnvIdFilterRequired()) {
      // TODO:
    }
    return true;
  }

  private boolean authFilters(Query query) {
    if (UserThreadLocal.get() == null || UserThreadLocal.get().getUserRequestInfo() == null) {
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

    return true;
  }

  @Override
  public Query createAuthorizedQuery(Class collectionClass) {
    Query query = createQuery(collectionClass);
    if (authFilters(query)) {
      return query;
    }
    throw new WingsException(getExceptionMsgWithUserContext());
  }

  @Override
  public Query createAuthorizedQuery(Class collectionClass, boolean disableValidation) {
    Query query = createQuery(collectionClass);
    if (disableValidation) {
      query.disableValidation();
    }
    if (authFilters(query)) {
      return query;
    }
    throw new WingsException(getExceptionMsgWithUserContext());
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
        String accountId = object.getAccountId();
        f.setAccessible(true);
        char[] secret = (char[]) f.get(object);
        Field encryptedField = getEncryptedRefField(f, object);
        encryptedField.setAccessible(true);
        encrypt(object, secret, encryptedField, savedObject, secretManager.getEncryptionType(accountId));
        f.set(object, null);
      }
    } catch (SecurityException e) {
      throw new WingsException("Security exception in encrypt", e);
    } catch (IllegalAccessException e) {
      throw new WingsException("Illegal access exception in encrypt", e);
    }
  }

  private String encrypt(Encryptable object, char[] secret, Field encryptedField, Encryptable savedObject,
      EncryptionType encryptionType) throws IllegalAccessException {
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
    String encryptedId =
        savedObject == null ? (String) encryptedField.get(object) : (String) encryptedField.get(savedObject);
    EncryptedData encryptedData = isBlank(encryptedId) ? null : get(EncryptedData.class, encryptedId);
    EncryptedData encryptedPair = secretManager.encrypt(
        encryptionType, accountId, object.getSettingType(), secret, encryptedData, UUID.randomUUID().toString());

    String changeLogDescription = "";

    if (encryptedData == null) {
      encryptedData = encryptedPair;
      encryptedData.setAccountId(accountId);
      encryptedData.setType(object.getSettingType());
      changeLogDescription = "Created";
    } else {
      encryptedData.setEncryptionKey(encryptedPair.getEncryptionKey());
      encryptedData.setEncryptedValue(encryptedPair.getEncryptedValue());
      encryptedData.setEncryptionType(encryptionType);
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

  private boolean isReferencedSecretText(Encryptable object, Field encryptedField) throws IllegalAccessException {
    if (ServiceVariable.class.isInstance(object)) {
      ServiceVariable serviceVariable = (ServiceVariable) object;
      if (isNotBlank(serviceVariable.getUuid())) {
        Field decryptedField = getDecryptedField(encryptedField, object);
        deleteEncryptionReference(object, Sets.newHashSet(decryptedField.getName()), serviceVariable.getUuid());
      }
      return true;
    }
    return false;
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
      if (fieldNames == null || fieldNames.contains(f.getName())) {
        Field encryptedField = getEncryptedRefField(f, object);
        encryptedField.setAccessible(true);
        String encryptedId;
        try {
          encryptedId = (String) encryptedField.get(object);
        } catch (IllegalAccessException e) {
          throw new WingsException("Could not deleter referenced record", e);
        }

        if (isBlank(encryptedId)) {
          continue;
        }

        EncryptedData encryptedData = get(EncryptedData.class, encryptedId);
        if (encryptedData == null) {
          continue;
        }
        encryptedData.removeParentId(parentId);
        if (encryptedData.getParentIds() != null && encryptedData.getParentIds().isEmpty()
            && encryptedData.getType() != SettingVariableTypes.SECRET_TEXT) {
          delete(encryptedData);
        } else {
          save(encryptedData);
        }
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
