package software.wings.dl;

import java.io.InputStream;
import java.util.AbstractMap.SimpleEntry;
import java.util.List;

import javax.inject.Inject;

import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;

import com.google.inject.Singleton;
import com.mongodb.WriteResult;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;

import software.wings.app.MainConfiguration;
import software.wings.beans.Base;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.SearchFilter;
import software.wings.beans.SearchFilter.OP;

@Singleton
public class WingsMongoPersistence implements WingsPersistence {
  private Datastore datastore;

  @Inject
  public WingsMongoPersistence(MainConfiguration configuration) {
    this.datastore = configuration.getMongoConnectionFactory().getDatastore();
  }

  @Override
  public <T extends Base> List<T> list(Class<T> cls) {
    return datastore.find(cls).asList();
  }

  @Override
  public <T extends Base> T get(Class<T> cls, String id) {
    return datastore.get(cls, id);
  }

  @Override
  public <T extends Base> T get(Class<T> cls, PageRequest<T> req) {
    PageResponse<T> res = MongoHelper.queryPageRequest(datastore, cls, req);
    if (res == null || res.getResponse() == null || res.getResponse().size() == 0) {
      return null;
    }
    return res.getResponse().get(0);
  }

  @Override
  public <T extends Base> String save(T t) {
    Key<T> key = datastore.save(t);
    return (String) key.getId();
  }

  @Override
  public <T extends Base> T saveAndGet(Class<T> cls, T t) {
    Object id = save(t);
    return datastore.get(cls, id);
  }

  @Override
  public <T extends Base> UpdateResults update(T ent, UpdateOperations<T> ops) {
    return datastore.update(ent, ops);
  }

  @Override
  public <T extends Base> boolean delete(Class<T> cls, String uuid) {
    WriteResult result = datastore.delete(cls, uuid);
    if (result == null || result.getN() == 0) {
      return false;
    }
    return true;
  }

  @Override
  public <T extends Base> boolean delete(T t) {
    WriteResult result = datastore.delete(t);
    if (result == null || result.getN() == 0) {
      return false;
    }
    return true;
  }

  public <T> List<T> query(Class<T> cls, SimpleEntry<String, String>... pairs) {
    PageRequest<T> req = new PageRequest<>();
    for (SimpleEntry<String, String> pair : pairs) {
      SearchFilter filter = new SearchFilter();
      filter.setFieldName(pair.getKey());
      filter.setFieldValue(pair.getValue());
      filter.setOp(OP.EQ);
      req.getFilters().add(filter);
    }
    return null;
  }
  @Override
  public <T> PageResponse<T> query(Class<T> cls, PageRequest<T> req) {
    return MongoHelper.queryPageRequest(datastore, cls, req);
  }

  @Override
  public String uploadFromStream(String bucketName, GridFSUploadOptions options, String filename, InputStream in) {
    GridFSBucket gridFSBucket =
        GridFSBuckets.create(datastore.getMongo().getDatabase(datastore.getDB().getName()), bucketName);
    ObjectId fileId = gridFSBucket.uploadFromStream(filename, in, options);
    return fileId.toHexString();
  }

  @Override
  public <T> UpdateOperations<T> createUpdateOperations(Class<T> cls) {
    return datastore.createUpdateOperations(cls);
  }

  @Override
  public <T> Query<T> createQuery(Class<T> cls) {
    return datastore.createQuery(cls);
  }

  @Override
  public <T> void update(Query<T> updateQuery, UpdateOperations<T> updateOperations) {
    datastore.update(updateQuery, updateOperations);
  }

  @Override
  public GridFSBucket createGridFSBucket(String bucketName) {
    return GridFSBuckets.create(datastore.getMongo().getDatabase(datastore.getDB().getName()), bucketName);
  }

  public Datastore getDatastore() {
    return datastore;
  }

  @Override
  public void close() {
    datastore.getMongo().close();
  }
}
