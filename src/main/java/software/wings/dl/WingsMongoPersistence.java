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
import software.wings.beans.ReadPref;
import software.wings.beans.SearchFilter;
import software.wings.beans.SearchFilter.Operator;
import software.wings.security.UserThreadLocal;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.inject.Inject;
import javax.inject.Named;

// TODO: Auto-generated Javadoc

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

  /* (non-Javadoc)
   * @see software.wings.dl.WingsPersistence#list(java.lang.Class)
   */
  @Override
  public <T extends Base> List<T> list(Class<T> cls) {
    return list(cls, ReadPref.NORMAL);
  }

  /* (non-Javadoc)
   * @see software.wings.dl.WingsPersistence#list(java.lang.Class, software.wings.beans.ReadPref)
   */
  @Override
  public <T extends Base> List<T> list(Class<T> cls, ReadPref readPref) {
    return datastoreMap.get(readPref).find(cls).asList();
  }

  /* (non-Javadoc)
   * @see software.wings.dl.WingsPersistence#get(java.lang.Class, java.lang.String)
   */
  @Override
  public <T extends Base> T get(Class<T> cls, String id) {
    return get(cls, id, ReadPref.NORMAL);
  }

  /* (non-Javadoc)
   * @see software.wings.dl.WingsPersistence#get(java.lang.Class, java.lang.String, java.lang.String)
   */
  @Override
  public <T extends Base> T get(Class<T> cls, String appId, String id) {
    return createQuery(cls).field("appId").equal(appId).field(ID_KEY).equal(id).field("active").equal(true).get();
  }

  /* (non-Javadoc)
   * @see software.wings.dl.WingsPersistence#get(java.lang.Class, java.lang.String, software.wings.beans.ReadPref)
   */
  @Override
  public <T extends Base> T get(Class<T> cls, String id, ReadPref readPref) {
    return datastoreMap.get(readPref).get(cls, id);
  }

  /* (non-Javadoc)
   * @see software.wings.dl.WingsPersistence#get(java.lang.Class, software.wings.dl.PageRequest)
   */
  @Override
  public <T extends Base> T get(Class<T> cls, PageRequest<T> req) {
    return get(cls, req, ReadPref.NORMAL);
  }

  /* (non-Javadoc)
   * @see software.wings.dl.WingsPersistence#get(java.lang.Class, software.wings.dl.PageRequest,
   * software.wings.beans.ReadPref)
   */
  @Override
  public <T extends Base> T get(Class<T> cls, PageRequest<T> req, ReadPref readPref) {
    req.addFilter(SearchFilter.Builder.aSearchFilter().withField("active", Operator.EQ, true).build());
    PageResponse<T> res = MongoHelper.queryPageRequest(datastoreMap.get(readPref), cls, req);
    if (isEmpty(res)) {
      return null;
    }
    return res.get(0);
  }

  /* (non-Javadoc)
   * @see software.wings.dl.WingsPersistence#save(software.wings.beans.Base)
   */
  @Override
  public <T extends Base> String save(T object) {
    Key<T> key = primaryDatastore.save(object);
    return (String) key.getId();
  }

  /* (non-Javadoc)
   * @see software.wings.dl.WingsPersistence#save(java.util.List)
   */
  @Override
  public <T extends Base> List<String> save(List<T> ts) {
    Iterable<Key<T>> keys = primaryDatastore.save(ts);
    List<String> ids = new ArrayList<>();
    keys.forEach(tKey -> ids.add((String) tKey.getId()));
    return ids;
  }

  /* (non-Javadoc)
   * @see software.wings.dl.WingsPersistence#saveAndGet(java.lang.Class, software.wings.beans.Base)
   */
  @Override
  public <T extends Base> T saveAndGet(Class<T> cls, T object) {
    Object id = save(object);
    return createQuery(cls).field("appId").equal(object.getAppId()).field(ID_KEY).equal(id).get();
  }

  /* (non-Javadoc)
   * @see software.wings.dl.WingsPersistence#update(org.mongodb.morphia.query.Query,
   * org.mongodb.morphia.query.UpdateOperations)
   */
  @Override
  public <T> void update(Query<T> updateQuery, UpdateOperations<T> updateOperations) {
    updateOperations.set("lastUpdatedAt", currentTimeMillis());
    if (UserThreadLocal.get() != null) {
      updateOperations.set("lastUpdatedBy", UserThreadLocal.get());
    }
    primaryDatastore.update(updateQuery, updateOperations);
  }

  /* (non-Javadoc)
   * @see software.wings.dl.WingsPersistence#update(software.wings.beans.Base,
   * org.mongodb.morphia.query.UpdateOperations)
   */
  @Override
  public <T extends Base> UpdateResults update(T ent, UpdateOperations<T> ops) {
    ops.set("lastUpdatedAt", currentTimeMillis());
    if (UserThreadLocal.get() != null) {
      ops.set("lastUpdatedBy", UserThreadLocal.get());
    }
    return primaryDatastore.update(ent, ops);
  }

  /* (non-Javadoc)
   * @see software.wings.dl.WingsPersistence#updateFields(java.lang.Class, java.lang.String, java.util.Map)
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

  /* (non-Javadoc)
   * @see software.wings.dl.WingsPersistence#addToList(java.lang.Class, java.lang.String, java.lang.String,
   * java.lang.String, java.lang.Object)
   */
  @Override
  public <T> void addToList(Class<T> cls, String appId, String entityId, String listFieldName, Object object) {
    primaryDatastore.update(createQuery(cls).field(ID_KEY).equal(entityId).field("appId").equal(appId),
        createUpdateOperations(cls).add(listFieldName, object));
  }

  /* (non-Javadoc)
   * @see software.wings.dl.WingsPersistence#addToList(java.lang.Class, java.lang.String, java.lang.String,
   * org.mongodb.morphia.query.Query, java.lang.String, java.lang.Object)
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

  /* (non-Javadoc)
   * @see software.wings.dl.WingsPersistence#addToList(java.lang.Class, java.lang.String, java.lang.String,
   * java.lang.Object)
   */
  @Override
  public <T> void addToList(Class<T> cls, String entityId, String listFieldName, Object object) {
    Query<T> query = primaryDatastore.createQuery(cls).field(ID_KEY).equal(entityId);
    UpdateOperations<T> operation = primaryDatastore.createUpdateOperations(cls).add(listFieldName, object);
    update(query, operation);
  }

  /* (non-Javadoc)
   * @see software.wings.dl.WingsPersistence#deleteFromList(java.lang.Class, java.lang.String, java.lang.String,
   * java.lang.Object)
   */
  @Override
  public <T> void deleteFromList(Class<T> cls, String entityId, String listFieldName, Object obj) {
    Query<T> query = primaryDatastore.createQuery(cls).field(ID_KEY).equal(entityId);
    UpdateOperations<T> operation = primaryDatastore.createUpdateOperations(cls).removeAll(listFieldName, obj);
    update(query, operation);
  }

  /* (non-Javadoc)
   * @see software.wings.dl.WingsPersistence#delete(java.lang.Class, java.lang.String)
   */
  @Override
  public <T extends Base> boolean delete(Class<T> cls, String uuid) {
    WriteResult result = primaryDatastore.delete(cls, uuid);
    return !(result == null || result.getN() == 0);
  }

  /* (non-Javadoc)
   * @see software.wings.dl.WingsPersistence#delete(org.mongodb.morphia.query.Query)
   */
  @Override
  public <T extends Base> boolean delete(Query<T> query) {
    WriteResult result = primaryDatastore.delete(query);
    return !(result == null || result.getN() == 0);
  }

  /* (non-Javadoc)
   * @see software.wings.dl.WingsPersistence#delete(software.wings.beans.Base)
   */
  @Override
  public <T extends Base> boolean delete(T object) {
    WriteResult result = primaryDatastore.delete(object);
    return !(result == null || result.getN() == 0);
  }

  /* (non-Javadoc)
   * @see software.wings.dl.WingsPersistence#query(java.lang.Class, software.wings.dl.PageRequest)
   */
  @Override
  public <T> PageResponse<T> query(Class<T> cls, PageRequest<T> req) {
    return query(cls, req, ReadPref.NORMAL);
  }

  /* (non-Javadoc)
   * @see software.wings.dl.WingsPersistence#query(java.lang.Class, software.wings.dl.PageRequest,
   * software.wings.beans.ReadPref)
   */
  @Override
  public <T> PageResponse<T> query(Class<T> cls, PageRequest<T> req, ReadPref readPref) {
    req.addFilter(SearchFilter.Builder.aSearchFilter().withField("active", Operator.EQ, true).build());
    return MongoHelper.queryPageRequest(datastoreMap.get(readPref), cls, req);
  }

  /* (non-Javadoc)
   * @see software.wings.dl.WingsPersistence#createUpdateOperations(java.lang.Class)
   */
  @Override
  public <T> UpdateOperations<T> createUpdateOperations(Class<T> cls) {
    return primaryDatastore.createUpdateOperations(cls);
  }

  /* (non-Javadoc)
   * @see software.wings.dl.WingsPersistence#createQuery(java.lang.Class)
   */
  @Override
  public <T> Query<T> createQuery(Class<T> cls) {
    return createQuery(cls, ReadPref.NORMAL);
  }

  /* (non-Javadoc)
   * @see software.wings.dl.WingsPersistence#createQuery(java.lang.Class, software.wings.beans.ReadPref)
   */
  @Override
  public <T> Query<T> createQuery(Class<T> cls, ReadPref readPref) {
    return datastoreMap.get(readPref).createQuery(cls);
  }

  /* (non-Javadoc)
   * @see software.wings.dl.WingsPersistence#getOrCreateGridFSBucket(java.lang.String)
   */
  @Override
  public GridFSBucket getOrCreateGridFSBucket(String bucketName) {
    return GridFSBuckets.create(
        primaryDatastore.getMongo().getDatabase(primaryDatastore.getDB().getName()), bucketName);
  }

  /* (non-Javadoc)
   * @see software.wings.dl.WingsPersistence#uploadFromStream(java.lang.String,
   * com.mongodb.client.gridfs.model.GridFSUploadOptions, java.lang.String, java.io.InputStream)
   */
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

  /* (non-Javadoc)
   * @see software.wings.dl.WingsPersistence#close()
   */
  @Override
  public void close() {
    primaryDatastore.getMongo().close();
  }

  /* (non-Javadoc)
   * @see software.wings.dl.WingsPersistence#getCollection(java.lang.String)
   */
  @Override
  public DBCollection getCollection(String collectionName) {
    return primaryDatastore.getDB().getCollection(collectionName);
  }

  /* (non-Javadoc)
   * @see io.dropwizard.lifecycle.Managed#start()
   */
  @Override
  public void start() throws Exception {
    // Do nothing
  }

  /* (non-Javadoc)
   * @see io.dropwizard.lifecycle.Managed#stop()
   */
  @Override
  public void stop() throws Exception {
    close();
  }
}
