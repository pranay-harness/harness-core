package software.wings.dl;

import static java.lang.System.currentTimeMillis;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.SearchFilter.Builder.aSearchFilter;
import static software.wings.utils.WingsReflectionUtils.getDeclaredAndInheritedFields;
import static software.wings.utils.WingsReflectionUtils.getDecryptedField;
import static software.wings.utils.WingsReflectionUtils.getEncryptedRefField;

import com.google.inject.Singleton;

import com.mongodb.DBCollection;
import com.mongodb.DuplicateKeyException;
import com.mongodb.WriteResult;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import io.dropwizard.lifecycle.Managed;
import org.apache.commons.lang3.StringUtils;
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
import software.wings.beans.KmsConfig;
import software.wings.beans.ReadPref;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.ServiceVariable;
import software.wings.beans.ServiceVariable.Type;
import software.wings.beans.SettingAttribute;
import software.wings.common.UUIDGenerator;
import software.wings.exception.WingsException;
import software.wings.security.EncryptionType;
import software.wings.security.UserRequestInfo;
import software.wings.security.UserThreadLocal;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.SecretChangeLog;
import software.wings.security.encryption.SimpleEncryption;
import software.wings.service.intfc.security.KmsService;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * The Class WingsMongoPersistence.
 */
@Singleton
public class WingsMongoPersistence implements WingsPersistence, Managed {
  @Inject private KmsService kmsService;
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
  public <T extends Base> T get(Class<T> cls, String id, ReadPref readPref) {
    T data = datastoreMap.get(readPref).get(cls, id);
    return data;
  }

  @Override
  public <T extends Base> T executeGetOneQuery(Query<T> query) {
    T data = query.get();
    return data;
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
    T data = res.get(0);
    return data;
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
    try {
      encryptIfNecessary(object);
      Key<T> key = primaryDatastore.save(object);
      updateParentIfNecessary(object, (String) key.getId());
      return (String) key.getId();
    } catch (Exception e) {
      throw new WingsException("Error saving " + object, e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T extends Base> List<String> save(List<T> ts) {
    for (Iterator<T> iterator = ts.iterator(); iterator.hasNext();) {
      T t = iterator.next();
      if (t == null) {
        iterator.remove();
      }
    }
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
    T data = createQuery(cls).field("appId").equal(object.getAppId()).field(ID_KEY).equal(id).get();
    return data;
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
        encrypt(e, (Encryptable) ((SettingAttribute) datastoreMap.get(ReadPref.NORMAL).get(cls, entityId)).getValue());
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

            if (shouldEncryptWhileUpdating(f, object, keyValuePairs)) {
              String accountId = object.getAccountId();
              if (kmsService.shouldUseKms(accountId)) {
                Field encryptedField = getEncryptedRefField(f, object);
                String encryptedId = encrypt(object, (char[]) value, encryptedField, null);
                operations.set(encryptedField.getName(), encryptedId);
                operations.unset(f.getName());
                continue;
              } else {
                char[] outputChars = new SimpleEncryption(accountId).encryptChars((char[]) value);
                value = outputChars;
              }
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

  private boolean shouldEncryptWhileUpdating(Field f, Encryptable object, Map<String, Object> keyValuePairs)
      throws IllegalAccessException {
    List<Field> encryptedFields = object.getEncryptedFields();
    if (object.getClass().equals(ServiceVariable.class)) {
      if (keyValuePairs.containsKey("type")) {
        if (keyValuePairs.get("type").equals(Type.ENCRYPTED_TEXT)) {
          return true;
        } else {
          deleteEncryptionReference(object, Collections.singleton(f.getName()));
          return false;
        }
      }
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
        try {
          deleteEncryptionReferenceIfNecessary(object);
        } catch (IllegalAccessException e) {
          throw new WingsException("Could not delete entity", e);
        }
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
      try {
        deleteEncryptionReferenceIfNecessary(object);
      } catch (IllegalAccessException e) {
        throw new WingsException("Could not delete entity", e);
      }
    }
    WriteResult result = primaryDatastore.delete(object);
    return !(result == null || result.getN() == 0);
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
    PageResponse<T> output = MongoHelper.queryPageRequest(datastoreMap.get(readPref), cls, req, false);
    return output;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> PageResponse<T> query(Class<T> cls, PageRequest<T> req, ReadPref readPref, boolean disableValidation) {
    if (!authFilters(req)) {
      return PageResponse.Builder.aPageResponse().withTotal(0).build();
    }
    PageResponse<T> output = MongoHelper.queryPageRequest(datastoreMap.get(readPref), cls, req, disableValidation);
    return output;
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

  private boolean isEmpty(List list) {
    return list == null || list.isEmpty();
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
    } else {
      throw new WingsException(
          "AuthFilter could not be applied since the user is not assigned to any apps / no app exists in the account");
    }
  }

  @Override
  public Query createAuthorizedQuery(Class collectionClass, boolean disableValidation) {
    Query query = createQuery(collectionClass);
    if (disableValidation) {
      query.disableValidation();
    }
    if (authFilters(query)) {
      return query;
    } else {
      throw new WingsException(
          "AuthFilter could not be applied since the user is not assigned to any apps / no app exists in the account");
    }
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
        if (kmsService.shouldUseKms(accountId)) {
          Field encryptedField = getEncryptedRefField(f, object);
          encrypt(object, secret, encryptedField, savedObject);
          f.set(object, null);
        } else {
          char[] outputChars = new SimpleEncryption(accountId).encryptChars((char[]) f.get(object));
          f.set(object, outputChars);
        }
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
    final String accountId = object.getAccountId();
    String encryptedId =
        savedObject == null ? (String) encryptedField.get(object) : (String) encryptedField.get(savedObject);
    EncryptedData encryptedData = StringUtils.isBlank(encryptedId) ? null : get(EncryptedData.class, encryptedId);
    final KmsConfig kmsConfig = kmsService.getKmsConfig(accountId);
    EncryptedData encryptedPair = kmsService.encrypt(secret, accountId, kmsConfig);
    String changeLogDescription = "";

    if (encryptedData == null) {
      encryptedData = encryptedPair;
      encryptedData.setAccountId(accountId);
      encryptedData.setType(object.getSettingType());
      changeLogDescription = "Created";
    } else {
      encryptedData.setEncryptionKey(encryptedPair.getEncryptionKey());
      encryptedData.setEncryptedValue(encryptedPair.getEncryptedValue());
      changeLogDescription = "Changed " + getDecryptedField(encryptedField, object).getName();
    }

    if (kmsConfig != null) {
      encryptedData.setKmsId(kmsConfig.getUuid());
    }
    encryptedData.setEncryptionType(EncryptionType.KMS);
    encryptedId = save(encryptedData);
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
    encryptedField.set(object, encryptedId);
    return encryptedId;
  }

  private void updateParent(Encryptable object, String parentId) throws IllegalAccessException {
    List<Field> fieldsToEncrypt = object.getEncryptedFields();
    for (Field f : fieldsToEncrypt) {
      f.setAccessible(true);
      // if the field was never encrypted using kms
      if (f.get(object) != null) {
        continue;
      }
      Field encryptedField = getEncryptedRefField(f, object);
      encryptedField.setAccessible(true);
      String encryptedId = (String) encryptedField.get(object);

      if (StringUtils.isBlank(encryptedId)) {
        continue;
      }

      EncryptedData encryptedData = get(EncryptedData.class, encryptedId);
      if (encryptedData == null) {
        continue;
      }

      if (StringUtils.isBlank(encryptedData.getParentId()) || !encryptedData.getParentId().equals(parentId)) {
        encryptedData.setParentId(parentId);
        save(encryptedData);
      }
    }
  }

  private void deleteEncryptionReference(Encryptable object, Set<String> fieldNames) throws IllegalAccessException {
    List<Field> fieldsToEncrypt = object.getEncryptedFields();
    for (Field f : fieldsToEncrypt) {
      if ((fieldNames == null || fieldNames.contains(f.getName()))) {
        f.setAccessible(true);
        // if the field was never encrypted using kms
        if (f.get(object) != null) {
          continue;
        }

        Field encryptedField = getEncryptedRefField(f, object);
        encryptedField.setAccessible(true);
        String encryptedId = (String) encryptedField.get(object);

        if (StringUtils.isBlank(encryptedId)) {
          continue;
        }

        EncryptedData encryptedData = get(EncryptedData.class, encryptedId);
        if (encryptedData == null) {
          continue;
        }

        delete(encryptedData);
      }
    }
  }

  private void decrypt(Encryptable object) {
    try {
      List<Field> encryptedFields = object.getEncryptedFields();
      for (Field f : encryptedFields) {
        f.setAccessible(true);
        String accountId = object.getAccountId();
        char[] input = (char[]) f.get(object);

        char[] outputChars;
        Field encryptedField = getEncryptedRefField(f, object);
        encryptedField.setAccessible(true);
        String encryptedId = (String) encryptedField.get(object);
        final EncryptedData encryptedData = get(EncryptedData.class, encryptedId);
        // this was encrypted using kms
        if (input == null && encryptedData != null) {
          final KmsConfig kmsConfig = kmsService.getKmsConfig(accountId);
          outputChars = kmsService.decrypt(encryptedData, accountId, kmsConfig);
        } else {
          SimpleEncryption encryption = new SimpleEncryption(accountId);
          outputChars = encryption.decryptChars(input);
        }
        f.set(object, outputChars);
      }
    } catch (SecurityException e) {
      throw new WingsException("Security exception in encrypt", e);
    } catch (IllegalAccessException e) {
      throw new WingsException("Illegal access exception in encrypt", e);
    }
  }

  private <T extends Base> void encryptIfNecessary(T o) {
    // if its an update
    Object savedObject = null;
    if (!StringUtils.isBlank(o.getUuid())
        && (SettingAttribute.class.isInstance(o) || Encryptable.class.isInstance(o))) {
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

  private void updateParentIfNecessary(Object o, String parentId) throws IllegalAccessException {
    if (SettingAttribute.class.isInstance(o)) {
      o = ((SettingAttribute) o).getValue();
    }

    if (Encryptable.class.isInstance(o)) {
      updateParent((Encryptable) o, parentId);
    }
  }

  private <T extends Base> void deleteEncryptionReferenceIfNecessary(T o) throws IllegalAccessException {
    if (StringUtils.isBlank(o.getUuid())) {
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
      deleteEncryptionReference((Encryptable) toDelete, null);
    }
  }
}
