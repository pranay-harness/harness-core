package software.wings.dl;

import static java.lang.System.currentTimeMillis;
import static org.eclipse.jetty.util.LazyList.isEmpty;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.EmbeddedUser.Builder.anEmbeddedUser;

import com.google.inject.Singleton;

import com.mongodb.DBCollection;
import com.mongodb.WriteResult;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import io.dropwizard.lifecycle.Managed;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
import software.wings.beans.Base;
import software.wings.beans.ReadPref;
import software.wings.beans.SortOrder;
import software.wings.beans.SortOrder.OrderType;
import software.wings.common.UUIDGenerator;
import software.wings.security.UserThreadLocal;

import java.util.ArrayList;
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
  private Datastore primaryDatastore;
  private Datastore secondaryDatastore;

  private Map<ReadPref, Datastore> datastoreMap;

  /**
   * Creates a new object for wings mongo persistence.
   *
   * @param primaryDatastore   primary datastore for critical reads and writes.
   * @param secondaryDatastore replica of primary for non critical reads.
   * @param datastoreMap       datastore map based on read preference to datastore.
   */
  @Inject
  public WingsMongoPersistence(@Named("primaryDatastore") Datastore primaryDatastore,
      @Named("secondaryDatastore") Datastore secondaryDatastore,
      @Named("datastoreMap") Map<ReadPref, Datastore> datastoreMap) {
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
    Key<T> key = primaryDatastore.save(object);
    return (String) key.getId();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T extends Base> List<String> save(List<T> ts) {
    Iterable<Key<T>> keys = primaryDatastore.save(ts);
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
    return createQuery(cls).field("appId").equal(object.getAppId()).field(ID_KEY).equal(id).get();
  }

  /**
   * {@inheritDoc}
   *
   * @return
   */
  @Override
  public <T> UpdateResults update(Query<T> updateQuery, UpdateOperations<T> updateOperations) {
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
    return primaryDatastore.findAndModify(query, updateOperations, false, true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T extends Base> UpdateResults update(T ent, UpdateOperations<T> ops) {
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
    Query<T> query = primaryDatastore.createQuery(cls).field(ID_KEY).equal(entityId);
    UpdateOperations<T> operations = primaryDatastore.createUpdateOperations(cls);
    for (Entry<String, Object> entry : keyValuePairs.entrySet()) {
      operations.set(entry.getKey(), entry.getValue());
    }
    update(query, operations);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> boolean addToList(
      Class<T> cls, String appId, String entityId, Query<T> query, String listFieldName, Object object) {
    return primaryDatastore
               .update(query.field(ID_KEY).equal(entityId).field("appId").equal(appId),
                   createUpdateOperations(cls).add(listFieldName, object))
               .getUpdatedCount()
        > 0;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> void deleteFromList(Class<T> cls, String appId, String entityId, String listFieldName, Object obj) {
    Query<T> query = primaryDatastore.createQuery(cls).field(ID_KEY).equal(entityId).field("appId").equal(appId);
    UpdateOperations<T> operation = primaryDatastore.createUpdateOperations(cls).removeAll(listFieldName, obj);
    update(query, operation);
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
  public <T> PageResponse<T> query(Class<T> cls, PageRequest<T> req, ReadPref readPref) {
    if (req.getOrders() == null || req.getOrders().size() == 0) {
      SortOrder sortOrder = new SortOrder();
      sortOrder.setFieldName("createdAt");
      sortOrder.setOrderType(OrderType.DESC);
      req.addOrder(sortOrder);
    }

    return MongoHelper.queryPageRequest(datastoreMap.get(readPref), cls, req);
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
  public Datastore getDatastore() {
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
}
