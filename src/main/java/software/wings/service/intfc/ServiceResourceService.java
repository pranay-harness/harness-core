package software.wings.service.intfc;

import com.mongodb.DBObject;
import software.wings.beans.FileMetadata;
import software.wings.beans.Service;
import software.wings.service.intfc.FileService.FileBucket;

import java.io.InputStream;
import java.util.List;

/**
 * Created by anubhaw on 3/28/16.
 */
public interface ServiceResourceService {
  public List<Service> list(String appID);
  public Service save(String appID, Service service);
  public Service findByUUID(String uuid);
  public Service update(Service service);
  String saveFile(String serviceID, FileMetadata fileMetadata, InputStream uploadedInputStream, FileBucket configs);
  List<DBObject> fetchConfigs(String serviceID);
}
