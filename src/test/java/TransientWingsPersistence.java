import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;

import com.mongodb.client.gridfs.model.GridFSUploadOptions;

import software.wings.beans.Base;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.dl.WingsPersistence;

public class TransientWingsPersistence implements WingsPersistence {
  private Map<Class, Map<String, Base>> persistentObjects = new HashMap<>();

  @Override
  public <T extends Base> String save(T t) {
    Map<String, Base> idMap = persistentObjects.get(t.getClass());
    if (idMap == null) {
      idMap = new HashMap<>();
      persistentObjects.put(t.getClass(), idMap);
    }
    idMap.put(t.getUuid(), t);
    return t.getUuid();
  }

  @Override
  public <T extends Base> T saveAndGet(Class<T> cls, T t) {
    save(t);
    return t;
  }

  @Override
  public <T extends Base> T get(Class<T> cls, String id) {
    Map<String, Base> idMap = persistentObjects.get(cls);
    if (idMap == null) {
      return null;
    }
    return (T) idMap.get(id);
  }

  @Override
  public <T extends Base> UpdateResults update(T ent, UpdateOperations<T> ops) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public <T> PageResponse<T> query(Class<T> cls, PageRequest<T> req) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String uploadFromStream(String bucketName, GridFSUploadOptions options, String filename, InputStream in) {
    // TODO Auto-generated method stub
    return null;
  }
}
