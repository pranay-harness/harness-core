package software.wings.service.intfc;

import org.hibernate.validator.constraints.NotEmpty;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.ConfigFile;
import software.wings.beans.ServiceTemplate;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.utils.validation.Create;
import software.wings.utils.validation.Update;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

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
  @ValidationGroups(Create.class) String save(@Valid ConfigFile configFile, InputStream inputStream);

  /**
   * Gets the.
   *
   * @param appId            the app id
   * @param configId         the config id
   * @param withOverridePath the with override path
   * @return the config file
   */
  ConfigFile get(@NotEmpty String appId, @NotEmpty String configId, @NotNull boolean withOverridePath);

  /**
   * Update.
   *
   * @param configFile  the config file
   * @param inputStream the input stream
   */
  @ValidationGroups(Update.class) void update(@Valid ConfigFile configFile, InputStream inputStream);

  /**
   * Delete.
   *
   * @param appId    the app id
   * @param configId the config id
   */
  void delete(@NotEmpty String appId, @NotEmpty String configId);

  /**
   * Gets the config files for entity.
   *
   * @param appId      the app id
   * @param templateId the template id
   * @param entityId   the entity id
   * @return the config files for entity
   */
  List<ConfigFile> getConfigFilesForEntity(String appId, String templateId, String entityId);

  /**
   * Delete by entity id.
   *
   * @param appId      the app id
   * @param entityId   the service id
   * @param templateId the template id
   */
  void deleteByEntityId(String appId, String entityId, String templateId);

  /**
   * Gets config file by template.
   *
   * @param appId           the app id
   * @param envId           the env id
   * @param serviceTemplate the service template
   * @return the config file by template
   */
  List<ConfigFile> getConfigFileByTemplate(String appId, String envId, ServiceTemplate serviceTemplate);

  /**
   * Download file.
   *
   * @param appId    the app id
   * @param configId the config id
   * @return the file
   */
  File download(String appId, String configId);
}
