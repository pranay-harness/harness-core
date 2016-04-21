package software.wings.dl;

import com.google.inject.Singleton;

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

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;

@Singleton
public class WingsMongoPersistence implements WingsPersistence, Managed {
  private Datastore primaryDatastore;
  private Datastore secondaryDatastore;

  private Map<ReadPref, Datastore> datastoreMap;

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
  public <T extends Base> T get(Class<T> cls, PageRequest<T> req) {
    return get(cls, req, ReadPref.NORMAL);
  }

  @Override
  public <T extends Base> T get(Class<T> cls, PageRequest<T> req, ReadPref readPref) {
    PageResponse<T> res = MongoHelper.queryPageRequest(datastoreMap.get(readPref), cls, req);
    if (res == null || res.getResponse() == null || res.getResponse().size() == 0) {
      return null;
    }
    return res.getResponse().get(0);
  }

  @Override
  public <T extends Base> T get(Class<T> cls, String id) {
    return get(cls, id, ReadPref.NORMAL);
  }

  @Override
  public <T extends Base> T get(Class<T> cls, String id, ReadPref readPref) {
    return datastoreMap.get(readPref).get(cls, id);
  }

  @Override
  public <T extends Base> String save(T t) {
    Key<T> key = primaryDatastore.save(t);
    return (String) key.getId();
  }

  @Override
  public <T extends Base> T saveAndGet(Class<T> cls, T t) {
    Object id = save(t);
    return primaryDatastore.get(cls, id);
  }

  @Override
  public <T extends Base> UpdateResults update(T ent, UpdateOperations<T> ops) {
    return primaryDatastore.update(ent, ops);
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
  public <T extends Base> boolean delete(T t) {
    WriteResult result = primaryDatastore.delete(t);
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
  public String uploadFromStream(String bucketName, GridFSUploadOptions options, String filename, InputStream in) {
    GridFSBucket gridFsBucket =
        GridFSBuckets.create(primaryDatastore.getMongo().getDatabase(primaryDatastore.getDB().getName()), bucketName);
    ObjectId fileId = gridFsBucket.uploadFromStream(filename, in, options);
    return fileId.toHexString();
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
  public <T> UpdateOperations<T> createUpdateOperations(Class<T> cls) {
    return primaryDatastore.createUpdateOperations(cls);
  }

  @Override
  public <T> void update(Query<T> updateQuery, UpdateOperations<T> updateOperations) {
    primaryDatastore.update(updateQuery, updateOperations);
  }

  @Override
  public GridFSBucket createGridFSBucket(String bucketName) {
    return GridFSBuckets.create(
        primaryDatastore.getMongo().getDatabase(primaryDatastore.getDB().getName()), bucketName);
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
  public void start() throws Exception {
    // Do nothing
  }

  @Override
  public void stop() throws Exception {
    close();
  }
}
