package software.wings.service.intfc;

import software.wings.beans.ConfigFile;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;

import java.io.InputStream;
import java.util.List;

/**
 * Created by anubhaw on 4/25/16.
 */
public interface ConfigService {
  PageResponse<ConfigFile> list(PageRequest<ConfigFile> request);

  String save(ConfigFile configFile, InputStream inputStream);

  ConfigFile get(String configId);

  void update(ConfigFile configFile, InputStream inputStream);

  void delete(String configId);

  List<ConfigFile> getConfigFilesForEntity(String templateId, String entityId);
}
