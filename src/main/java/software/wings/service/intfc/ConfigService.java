package software.wings.service.intfc;

import software.wings.beans.ConfigFile;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;

import java.io.InputStream;
import java.util.List;

// TODO: Auto-generated Javadoc

/**
 * Created by anubhaw on 4/25/16.
 */
public interface ConfigService {
  /**
   * List.
   *
   * @param request the request
   * @return the page response
   */
  PageResponse<ConfigFile> list(PageRequest<ConfigFile> request);

  /**
   * Save.
   *
   * @param configFile  the config file
   * @param inputStream the input stream
   * @return the string
   */
  String save(ConfigFile configFile, InputStream inputStream);

  /**
   * Gets the.
   *
   * @param configId the config id
   * @return the config file
   */
  ConfigFile get(String configId);

  /**
   * Update.
   *
   * @param configFile  the config file
   * @param inputStream the input stream
   */
  void update(ConfigFile configFile, InputStream inputStream);

  /**
   * Delete.
   *
   * @param configId the config id
   */
  void delete(String configId);

  /**
   * Gets the config files for entity.
   *
   * @param templateId the template id
   * @param entityId   the entity id
   * @return the config files for entity
   */
  List<ConfigFile> getConfigFilesForEntity(String templateId, String entityId);

  /**
   * Delete by entity id.
   *
   * @param appId      the app id
   * @param entityId   the service id
   * @param templateId the template id
   */
  void deleteByEntityId(String appId, String entityId, String templateId);
}
