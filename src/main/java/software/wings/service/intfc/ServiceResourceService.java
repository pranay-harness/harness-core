package software.wings.service.intfc;

import software.wings.beans.ConfigFile;
import software.wings.beans.Service;
import software.wings.service.intfc.FileService.FileBucket;

import java.io.InputStream;
import java.util.List;

/**
 * Created by anubhaw on 3/28/16.
 */
public interface ServiceResourceService {
  List<Service> list(String appID);
  Service save(String appID, Service service);
  Service findByUUID(String uuid);
  Service update(Service service);
  List<ConfigFile> getConfigs(String serviceID);
  String saveFile(ConfigFile configFile, InputStream uploadedInputStream, FileBucket configs);
  ConfigFile getConfig(String configID);
  void updateFile(ConfigFile configFile, InputStream uploadedInputStream, FileBucket configs);
}
