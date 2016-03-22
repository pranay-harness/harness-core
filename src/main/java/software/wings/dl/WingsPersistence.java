package software.wings.dl;

import java.io.InputStream;

import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;

import com.mongodb.client.gridfs.model.GridFSUploadOptions;

import software.wings.beans.Base;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;

public interface WingsPersistence {
  public <T extends Base> T get(Class<T> cls, String id);

  public <T extends Base> T get(Class<T> cls, PageRequest<T> req);

  public <T extends Base> String save(T t);

  public <T extends Base> T saveAndGet(Class<T> cls, T t);

  public <T extends Base> UpdateResults update(T ent, UpdateOperations<T> ops);

  public <T extends Base> boolean delete(Class<T> cls, String uuid);

  public <T extends Base> boolean delete(T entity);

  public <T> PageResponse<T> query(Class<T> cls, PageRequest<T> req);

  public String uploadFromStream(String bucketName, GridFSUploadOptions options, String filename, InputStream in);

  public <T> UpdateOperations<T> createUpdateOperations(Class<T> cls);

  public void close();
}
