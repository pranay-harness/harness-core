package software.wings.dl;

import java.io.InputStream;
import java.util.List;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;

import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;

import software.wings.beans.Base;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.ReadPref;

public interface WingsPersistence {
  public <T extends Base> List<T> list(Class<T> cls);

  public <T extends Base> List<T> list(Class<T> cls, ReadPref readPref);

  public <T extends Base> T get(Class<T> cls, PageRequest<T> req);

  public <T extends Base> T get(Class<T> cls, PageRequest<T> req, ReadPref readPref);

  public <T extends Base> T get(Class<T> cls, String id);

  public <T extends Base> T get(Class<T> cls, String id, ReadPref readPref);

  public <T extends Base> String save(T t);

  public <T extends Base> T saveAndGet(Class<T> cls, T t);

  public <T extends Base> UpdateResults update(T ent, UpdateOperations<T> ops);

  public <T extends Base> boolean delete(Class<T> cls, String uuid);

  public <T extends Base> boolean delete(T entity);

  public <T> PageResponse<T> query(Class<T> cls, PageRequest<T> req);

  public <T> PageResponse<T> query(Class<T> cls, PageRequest<T> req, ReadPref readPref);

  public String uploadFromStream(String bucketName, GridFSUploadOptions options, String filename, InputStream in);

  public <T> Query<T> createQuery(Class<T> cls);

  public <T> Query<T> createQuery(Class<T> cls, ReadPref readPref);

  public <T> UpdateOperations<T> createUpdateOperations(Class<T> cls);

  public <T> void update(Query<T> updateQuery, UpdateOperations<T> updateOperations);

  public GridFSBucket createGridFSBucket(String bucketName);

  public Datastore getDatastore();

  public void close();
}
