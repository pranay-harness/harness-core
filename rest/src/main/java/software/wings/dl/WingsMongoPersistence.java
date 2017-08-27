package software.wings.dl;

import static java.lang.System.currentTimeMillis;
import static org.eclipse.jetty.util.LazyList.isEmpty;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.EmbeddedUser.Builder.anEmbeddedUser;
import static software.wings.beans.SearchFilter.Builder.aSearchFilter;

import com.google.inject.Singleton;

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
import software.wings.beans.Base;
import software.wings.beans.ReadPref;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.SettingAttribute;
import software.wings.common.UUIDGenerator;
import software.wings.exception.WingsException;
import software.wings.security.UserRequestInfo;
import software.wings.security.UserThreadLocal;
import software.wings.security.annotations.Encrypted;
import software.wings.security.encryption.Encryptable;
import software.wings.security.encryption.SimpleEncryption;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * The Class WingsMongoPersistence.
 */
@Singleton
public class WingsMongoPersistence implements WingsPersistence, Managed {
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
    if (SettingAttribute.class.isInstance(data)) {
      this.decryptIfNecessary(((SettingAttribute) data).getValue());
    } else if (data instanceof Encryptable) {
      this.decryptIfNecessary(data);
    }
    return data;
  }

  @Override
  public <T extends Base> T executeGetOneQuery(Query<T> query) {
    T data = query.get();
    if (SettingAttribute.class.isInstance(data)) {
      this.decryptIfNecessary(((SettingAttribute) data).getValue());
    } else if (data instanceof Encryptable) {
      this.decryptIfNecessary(data);
    }
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
    if (SettingAttribute.class.isInstance(data)) {
      this.decryptIfNecessary(((SettingAttribute) data).getValue());
    } else if (data instanceof Encryptable) {
      this.decryptIfNecessary(data);
    }
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
    if (SettingAttribute.class.isInstance(object)) {
      this.encryptIfNecessary(((SettingAttribute) object).getValue());
    } else if (object instanceof Encryptable) {
      this.encryptIfNecessary(object);
    }
    Key<T> key = primaryDatastore.save(object);
    if (SettingAttribute.class.isInstance(object)) {
      this.decryptIfNecessary(((SettingAttribute) object).getValue());
    } else if (object instanceof Encryptable) {
      this.decryptIfNecessary(object);
    }
    return (String) key.getId();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T extends Base> List<String> save(List<T> ts) {
    for (T t : ts) {
      if (SettingAttribute.class.isInstance(t)) {
        this.encryptIfNecessary(((SettingAttribute) t).getValue());
      } else if (t instanceof Encryptable) {
        this.encryptIfNecessary(t);
      }
    }
    Iterable<Key<T>> keys = primaryDatastore.save(ts);
    for (T t : ts) {
      if (SettingAttribute.class.isInstance(t)) {
        this.decryptIfNecessary(((SettingAttribute) t).getValue());
      } else if (t instanceof Encryptable) {
        this.decryptIfNecessary(t);
      }
    }
    List<String> ids = new ArrayList<>();
    keys.forEach(tKey -> ids.add((String) tKey.getId()));
    return ids;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T extends Base> List<String> saveIgnoringDuplicateKeys(List<T> ts) {
    for (T t : ts) {
      if (SettingAttribute.class.isInstance(t)) {
        this.encryptIfNecessary(((SettingAttribute) t).getValue());
      } else if (t instanceof Encryptable) {
        this.encryptIfNecessary(t);
      }
    }
    InsertOptions insertOptions = new InsertOptions();
    insertOptions.continueOnError(true);
    Iterable<Key<T>> keys = new ArrayList<>();
    try {
      keys = primaryDatastore.insert(ts, insertOptions);
    } catch (DuplicateKeyException dke) {
      // ignore
    }
    for (T t : ts) {
      if (SettingAttribute.class.isInstance(t)) {
        this.decryptIfNecessary(((SettingAttribute) t).getValue());
      } else if (t instanceof Encryptable) {
        this.decryptIfNecessary(t);
      }
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
    if (SettingAttribute.class.isInstance(data)) {
      this.decryptIfNecessary(((SettingAttribute) data).getValue());
    } else if (data instanceof Encryptable) {
      this.decryptIfNecessary(data);
    }
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
          anEmbeddedUser()
              .withUuid(UserThreadLocal.get().getUuid())
              .withEmail(UserThreadLocal.get().getEmail())
              .withName(UserThreadLocal.get().getName())
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
          anEmbeddedUser()
              .withUuid(UserThreadLocal.get().getUuid())
              .withEmail(UserThreadLocal.get().getEmail())
              .withName(UserThreadLocal.get().getName())
              .build());
      updateOperations.setOnInsert("createdBy",
          anEmbeddedUser()
              .withUuid(UserThreadLocal.get().getUuid())
              .withEmail(UserThreadLocal.get().getEmail())
              .withName(UserThreadLocal.get().getName())
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
          anEmbeddedUser()
              .withUuid(UserThreadLocal.get().getUuid())
              .withEmail(UserThreadLocal.get().getEmail())
              .withName(UserThreadLocal.get().getName())
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
    this.updateFields(cls, entityId, keyValuePairs, null);
  }

  public <T> void updateFields(Class<T> cls, String entityId, Map<String, Object> keyValuePairs, String encryptionKey) {
    Query<T> query = primaryDatastore.createQuery(cls).field(ID_KEY).equal(entityId);
    UpdateOperations<T> operations = primaryDatastore.createUpdateOperations(cls);
    boolean encryptAfterUpdate = false;
    boolean encryptable = Encryptable.class.isAssignableFrom(cls);
    List<Field> declaredAndInheritedFields = getDeclaredAndInheritedFields(cls);
    for (Entry<String, Object> entry : keyValuePairs.entrySet()) {
      Object value = entry.getValue();
      if (cls == SettingAttribute.class && entry.getKey().equalsIgnoreCase("value")
          && Encryptable.class.isInstance(value)) {
        Encryptable e = (Encryptable) value;
        this.encrypt(e);
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
          value = this.encryptChars((char[]) value, encryptionKey);
          encryptAfterUpdate = true;
        }
      }
      operations.set(entry.getKey(), value);
    }

    update(query, operations);
    if (encryptAfterUpdate) {
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T extends Base> boolean delete(Class<T> cls, String uuid) {
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
    WriteResult result = primaryDatastore.delete(query);
    return !(result == null || result.getN() == 0);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T extends Base> boolean delete(T object) {
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
    for (T data : output.getResponse()) {
      if (SettingAttribute.class.isInstance(data)) {
        this.decryptIfNecessary(((SettingAttribute) data).getValue());
      } else if (data instanceof Encryptable) {
        this.decryptIfNecessary(data);
      }
    }
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
    for (T data : output.getResponse()) {
      if (SettingAttribute.class.isInstance(data)) {
        this.decryptIfNecessary(((SettingAttribute) data).getValue());
      } else if (data instanceof Encryptable) {
        this.decryptIfNecessary(data);
      }
    }
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
      if (userRequestInfo.getAppId() == null
          && (userRequestInfo.getAllowedAppIds() == null || userRequestInfo.getAllowedAppIds().isEmpty())) {
        return false;
      } else if (userRequestInfo.getAppId() == null && !userRequestInfo.getAllowedAppIds().isEmpty()) {
        pageRequest.addFilter(
            aSearchFilter().withField("appId", Operator.IN, userRequestInfo.getAllowedAppIds().toArray()).build());
      } else {
        pageRequest.addFilter(aSearchFilter().withField("appId", Operator.IN, userRequestInfo.getAppId()).build());
      }
    } else if (userRequestInfo.isEnvIdFilterRequired()) {
      // TODO:
    }
    return true;
  }

  /**
   * Encrypt an Encryptable object. Currently assumes SimpleEncryption.
   *
   * @param object the object to be encrypted
   */
  private void encrypt(Encryptable object) {
    try {
      List<Field> declaredAndInheritedFields = getDeclaredAndInheritedFields(object.getClass());
      for (Field f : declaredAndInheritedFields) {
        Encrypted a = f.getAnnotation(Encrypted.class);
        if (a != null && a.value()) {
          f.setAccessible(true);
          String accountId = object.getAccountId();
          char[] outputChars = this.encryptChars((char[]) f.get(object), accountId);
          f.set(object, outputChars);
        }
      }
    } catch (SecurityException e) {
      throw new WingsException("Security exception in encrypt", e);
    } catch (IllegalAccessException e) {
      throw new WingsException("Illegal access exception in encrypt", e);
    }
  }

  private char[] encryptChars(char[] content, String accountId) {
    SimpleEncryption encryption = new SimpleEncryption(accountId);
    return encryption.encryptChars(content);
  }

  private void decrypt(Encryptable object) {
    try {
      List<Field> declaredAndInheritedFields = getDeclaredAndInheritedFields(object.getClass());
      for (Field f : declaredAndInheritedFields) {
        Encrypted a = f.getAnnotation(Encrypted.class);
        if (a != null && a.value()) {
          f.setAccessible(true);
          char[] input = (char[]) f.get(object);
          String accountId = object.getAccountId();
          SimpleEncryption encryption = new SimpleEncryption(accountId);
          char[] outputChars = encryption.decryptChars(input);
          // This is a quirk of Mongo where if we insert a char[] and then retrieve it, it's wrapped
          // in quotes, so ['f'] becomes ['"', 'f', '"']. The logic is going to be moved out of the
          // Mongo layer, so I'm not going to take the time to modify the Mongo config to make this
          // stop happening.
          if (outputChars != null && outputChars.length > 0 && outputChars[0] == '"'
              && outputChars[outputChars.length - 1] == '"') {
            char[] copy = new char[outputChars.length - 2];
            System.arraycopy(outputChars, 1, copy, 0, outputChars.length - 2);
            outputChars = copy;
          }
          f.set(object, outputChars);
        }
      }
    } catch (SecurityException e) {
      throw new WingsException("Security exception in encrypt", e);
    } catch (IllegalAccessException e) {
      throw new WingsException("Illegal access exception in encrypt", e);
    }
  }

  private List<Field> getDeclaredAndInheritedFields(Class<?> clazz) {
    List<Field> declaredFields = new ArrayList<>();
    while (clazz.getSuperclass() != null) {
      Collections.addAll(declaredFields, clazz.getDeclaredFields());
      clazz = clazz.getSuperclass();
    }
    return declaredFields;
  }

  /**
   * Only to be used in testing.
   */
  @Override
  public <T extends Base> T getWithoutDecryptingTestOnly(Class<T> cls, String id) {
    T data = datastoreMap.get(ReadPref.NORMAL).get(cls, id);
    return data;
  }

  private void decryptIfNecessary(Object o) {
    if (Encryptable.class.isInstance(o)) {
      decrypt((Encryptable) o);
    }
  }

  private void encryptIfNecessary(Object o) {
    if (Encryptable.class.isInstance(o)) {
      encrypt((Encryptable) o);
    }
  }
}
