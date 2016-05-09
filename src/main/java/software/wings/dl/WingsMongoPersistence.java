package software.wings.dl;

import static java.lang.System.currentTimeMillis;
import static org.eclipse.jetty.util.LazyList.isEmpty;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;

import com.google.inject.Singleton;

import com.mongodb.DBCollection;
import com.mongodb.WriteResult;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import io.dropwizard.lifecycle.Managed;
import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
import software.wings.beans.Base;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.ReadPref;
import software.wings.security.UserThreadLocal;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Named;

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

  @Override
  public <T extends Base> List<T> list(Class<T> cls) {
    return list(cls, ReadPref.NORMAL);
  }

  @Override
  public <T extends Base> List<T> list(Class<T> cls, ReadPref readPref) {
    return datastoreMap.get(readPref).find(cls).asList();
  }

  @Override
  public <T extends Base> T get(Class<T> cls, String id) {
    return get(cls, id, ReadPref.NORMAL);
  }

  @Override
  public <T extends Base> T get(Class<T> cls, String applicationId, String id) {
    return createQuery(cls).field("applicationId").equal(applicationId).field("uuid").equal(id).get();
  }

  @Override
  public <T extends Base> T get(Class<T> cls, String id, ReadPref readPref) {
    return datastoreMap.get(readPref).get(cls, id);
  }

  @Override
  public <T extends Base> T get(Class<T> cls, PageRequest<T> req) {
    return get(cls, req, ReadPref.NORMAL);
  }

  @Override
  public <T extends Base> T get(Class<T> cls, PageRequest<T> req, ReadPref readPref) {
    PageResponse<T> res = MongoHelper.queryPageRequest(datastoreMap.get(readPref), cls, req);
    if (isEmpty(res)) {
      return null;
    }
    return res.get(0);
  }

  @Override
  public <T extends Base> String save(T object) {
    Key<T> key = primaryDatastore.save(object);
    return (String) key.getId();
  }

  @Override
  public <T extends Base> List<String> save(List<T> ts) {
    Iterable<Key<T>> keys = primaryDatastore.save(ts);
    List<String> ids = new ArrayList<>();
    keys.forEach(tKey -> ids.add((String) tKey.getId()));
    return ids;
  }

  @Override
  public <T extends Base> T saveAndGet(Class<T> cls, T object) {
    Object id = save(object);
    return primaryDatastore.get(cls, id);
  }

  @Override
  public <T> void update(Query<T> updateQuery, UpdateOperations<T> updateOperations) {
    updateOperations.set("lastUpdatedAt", currentTimeMillis());
    if (UserThreadLocal.get() != null) {
      updateOperations.set("lastUpdatedBy", UserThreadLocal.get());
    }
    primaryDatastore.update(updateQuery, updateOperations);
  }

  @Override
  public <T extends Base> UpdateResults update(T ent, UpdateOperations<T> ops) {
    ops.set("lastUpdatedAt", currentTimeMillis());
    if (UserThreadLocal.get() != null) {
      ops.set("lastUpdatedBy", UserThreadLocal.get());
    }
    return primaryDatastore.update(ent, ops);
  }

  @Override
  public <T> void updateFields(Class<T> cls, String entityId, Map<String, Object> keyValuePairs) {
    Query<T> query = primaryDatastore.createQuery(cls).field(ID_KEY).equal(entityId);
    UpdateOperations<T> operations = primaryDatastore.createUpdateOperations(cls);
    for (Entry<String, Object> entry : keyValuePairs.entrySet()) {
      operations.set(entry.getKey(), entry.getValue());
    }
    update(query, operations);
  }

  @Override
  public <T> void addToList(Class<T> cls, String entityId, String listFieldName, Object object) {
    Query<T> query = primaryDatastore.createQuery(cls).field(ID_KEY).equal(entityId);
    UpdateOperations<T> operation = primaryDatastore.createUpdateOperations(cls).add(listFieldName, object);
    update(query, operation);
  }

  @Override
  public <T> void deleteFromList(Class<T> cls, String entityId, String listFieldName, Object obj) {
    Query<T> query = primaryDatastore.createQuery(cls).field(ID_KEY).equal(entityId);
    UpdateOperations<T> operation = primaryDatastore.createUpdateOperations(cls).removeAll(listFieldName, obj);
    update(query, operation);
  }

  @Override
  public <T extends Base> boolean delete(Class<T> cls, String uuid) {
    WriteResult result = primaryDatastore.delete(cls, uuid);
    if (result == null || result.getN() == 0) {
      return false;
    }
    return true;
  }

  @Override
  public <T extends Base> boolean delete(T object) {
    WriteResult result = primaryDatastore.delete(object);
    if (result == null || result.getN() == 0) {
      return false;
    }
    return true;
  }

  @Override
  public <T> PageResponse<T> query(Class<T> cls, PageRequest<T> req) {
    return query(cls, req, ReadPref.NORMAL);
  }

  @Override
  public <T> PageResponse<T> query(Class<T> cls, PageRequest<T> req, ReadPref readPref) {
    return MongoHelper.queryPageRequest(datastoreMap.get(readPref), cls, req);
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
  public <T> Query<T> createQuery(Class<T> cls, ReadPref readPref) {
    return datastoreMap.get(readPref).createQuery(cls);
  }

  @Override
  public GridFSBucket createGridFSBucket(String bucketName) {
    return GridFSBuckets.create(
        primaryDatastore.getMongo().getDatabase(primaryDatastore.getDB().getName()), bucketName);
  }

  @Override
  public String uploadFromStream(String bucketName, GridFSUploadOptions options, String filename, InputStream in) {
    GridFSBucket gridFsBucket =
        GridFSBuckets.create(primaryDatastore.getMongo().getDatabase(primaryDatastore.getDB().getName()), bucketName);
    ObjectId fileId = gridFsBucket.uploadFromStream(filename, in, options);
    return fileId.toHexString();
  }

  @Override
  public Datastore getDatastore() {
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
}
