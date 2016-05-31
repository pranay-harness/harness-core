package software.wings.dl;

import com.mongodb.DBCollection;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
import software.wings.beans.Base;
import software.wings.beans.ReadPref;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface WingsPersistence {
  <T extends Base> List<T> list(Class<T> cls);

  <T extends Base> List<T> list(Class<T> cls, ReadPref readPref);

  <T extends Base> T get(Class<T> cls, PageRequest<T> req);

  <T extends Base> T get(Class<T> cls, PageRequest<T> req, ReadPref readPref);

  <T extends Base> T get(Class<T> cls, String id);

  <T extends Base> T get(Class<T> cls, String appId, String id);

  <T extends Base> T get(Class<T> cls, String id, ReadPref readPref);

  <T extends Base> List<String> save(List<T> tList);

  <T extends Base> String save(T t);

  <T extends Base> T saveAndGet(Class<T> cls, T t);

  <T> UpdateOperations<T> createUpdateOperations(Class<T> cls);

  <T extends Base> UpdateResults update(T ent, UpdateOperations<T> ops);

  <T> void update(Query<T> updateQuery, UpdateOperations<T> updateOperations);

  <T> void updateFields(Class<T> cls, String entityId, Map<String, Object> keyValuePairs);

  <T> void addToListIfNotExists(Class<T> cls, String appId, String entityId, String listFieldName, Object object);

  <T> void compareAndAddToList(
      Class<T> cls, String appId, String entityId, String listFieldName, Object object, List oldList);

  <T> void addToList(Class<T> cls, String entityId, String listFieldName, Object object);

  <T> void deleteFromList(Class<T> cls, String entityId, String listFieldName, Object object);

  <T extends Base> boolean delete(Class<T> cls, String uuid);

  <T extends Base> boolean delete(Query<T> query);

  <T extends Base> boolean delete(T entity);

  <T> PageResponse<T> query(Class<T> cls, PageRequest<T> req);

  <T> PageResponse<T> query(Class<T> cls, PageRequest<T> req, ReadPref readPref);

  String uploadFromStream(String bucketName, GridFSUploadOptions options, String filename, InputStream in);

  <T> Query<T> createQuery(Class<T> cls);

  <T> Query<T> createQuery(Class<T> cls, ReadPref readPref);

  GridFSBucket getOrCreateGridFSBucket(String bucketName);

  Datastore getDatastore();

  DBCollection getCollection(String collectionName);

  void close();
}
