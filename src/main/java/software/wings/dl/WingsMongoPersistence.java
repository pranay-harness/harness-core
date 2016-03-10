package software.wings.dl;

import java.io.InputStream;
import java.util.AbstractMap.SimpleEntry;
import java.util.List;

import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;

import com.mongodb.WriteResult;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;

import software.wings.beans.Base;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.Release;
import software.wings.beans.SearchFilter;
import software.wings.beans.SearchFilter.OP;

public class WingsMongoPersistence implements WingsPersistence {
  private Datastore datastore;

  public WingsMongoPersistence(Datastore datastore) {
    this.datastore = datastore;
  }

  public <T extends Base> T get(Class<T> cls, String id) {
    return (T) datastore.get(Release.class, id);
  }

  public <T extends Base> T get(Class<T> cls, PageRequest<T> req) {
    PageResponse<T> res = MongoHelper.queryPageRequest(datastore, cls, req);
    if (res == null || res.getResponse() == null || res.getResponse().size() == 0) {
      return null;
    }
    return res.getResponse().get(0);
  }

  public <T extends Base> String save(T t) {
    Key<T> key = datastore.save(t);
    return (String) key.getId();
  }

  public <T extends Base> T saveAndGet(Class<T> cls, T t) {
    Object id = save(t);
    return datastore.get(cls, id);
  }

  public <T extends Base> UpdateResults update(T ent, UpdateOperations<T> ops) {
    return datastore.update(ent, ops);
  }

  public <T extends Base> boolean delete(Class<T> cls, String uuid) {
    WriteResult result = datastore.delete(cls, uuid);
    if (result == null || result.getN() == 0) {
      return false;
    }
    return true;
  }

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
  public <T> PageResponse<T> query(Class<T> cls, PageRequest<T> req) {
    return MongoHelper.queryPageRequest(datastore, cls, req);
  }

  public String uploadFromStream(String bucketName, GridFSUploadOptions options, String filename, InputStream in) {
    GridFSBucket gridFSBucket =
        GridFSBuckets.create(datastore.getMongo().getDatabase(datastore.getDB().getName()), bucketName);
    ObjectId fileId = gridFSBucket.uploadFromStream(filename, in, options);
    return fileId.toHexString();
  }
}
