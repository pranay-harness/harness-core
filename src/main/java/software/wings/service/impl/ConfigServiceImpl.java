package software.wings.service.impl;

import static software.wings.beans.ErrorCodes.UNKNOWN_ERROR;
import static software.wings.service.intfc.FileService.FileBucket.CONFIGS;

import software.wings.beans.ConfigFile;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.TagService;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.executable.ValidateOnExecution;

// TODO: Auto-generated Javadoc

/**
 * Created by anubhaw on 4/25/16.
 */
@ValidateOnExecution
@Singleton
public class ConfigServiceImpl implements ConfigService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private FileService fileService;
  @Inject private EnvironmentService environmentService;
  @Inject private TagService tagService;
  @Inject private HostService hostService;
  @Inject private ServiceResourceService serviceResourceService;

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ConfigService#list(software.wings.dl.PageRequest)
   */
  @Override
  public PageResponse<ConfigFile> list(PageRequest<ConfigFile> request) {
    return wingsPersistence.query(ConfigFile.class, request);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ConfigService#save(software.wings.beans.ConfigFile, java.io.InputStream)
   */
  @Override
  public String save(ConfigFile configFile, InputStream inputStream) {
    fileService.saveFile(configFile, inputStream, CONFIGS);
    return wingsPersistence.save(configFile);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ConfigService#get(java.lang.String)
   */
  @Override
  public ConfigFile get(String appId, String configId) {
    ConfigFile configFile = wingsPersistence.get(ConfigFile.class, appId, configId);
    configFile.setOverridePath(generateOverridePath(configFile));
    return configFile;
  }

  private String generateOverridePath(ConfigFile configFile) {
    switch (configFile.getEntityType()) {
      case SERVICE:
        return serviceResourceService.get(configFile.getAppId(), configFile.getEntityId()).getName();
      case ENVIRONMENT:
        return environmentService.get(configFile.getAppId(), configFile.getEntityId(), false).getName();
      case TAG:
        return tagService.get(configFile.getAppId(), configFile.getEnvId(), configFile.getEntityId()).getName();
      case HOST:
        return hostService.getHostByEnv(configFile.getAppId(), configFile.getEnvId(), configFile.getEntityId())
            .getHostName();
      default:
        throw new WingsException(UNKNOWN_ERROR);
    }
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ConfigService#update(software.wings.beans.ConfigFile, java.io.InputStream)
   */
  @Override
  public void update(ConfigFile configFile, InputStream uploadedInputStream) {
    if (uploadedInputStream != null) {
      fileService.saveFile(configFile, uploadedInputStream, CONFIGS);
    }
    Map<String, Object> updateMap = new HashMap<>();
    updateMap.put("name", configFile.getName());
    updateMap.put("relativePath", configFile.getRelativePath());
    if (configFile.getChecksum() != null) {
      updateMap.put("checksum", configFile.getChecksum());
    }
    wingsPersistence.updateFields(ConfigFile.class, configFile.getUuid(), updateMap);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ConfigService#delete(java.lang.String)
   */
  @Override
  public void delete(String appId, String configId) {
    ConfigFile configFile = wingsPersistence.get(ConfigFile.class, appId, configId);
    fileService.deleteFile(configFile.getFileUuid(), CONFIGS);
    wingsPersistence.delete(configFile);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ConfigService#getConfigFilesForEntity(java.lang.String, java.lang.String)
   */
  @Override
  public List<ConfigFile> getConfigFilesForEntity(String appId, String templateId, String entityId) {
    return wingsPersistence.createQuery(ConfigFile.class)
        .field("appId")
        .equal(appId)
        .field("templateId")
        .equal(templateId)
        .field("entityId")
        .equal(entityId)
        .asList();
  }

  @Override
  public void deleteByEntityId(String appId, String entityId, String templateId) {
    List<ConfigFile> configFiles = wingsPersistence.createQuery(ConfigFile.class)
                                       .field("appId")
                                       .equal(appId)
                                       .field("entityId")
                                       .equal(entityId)
                                       .field("templateId")
                                       .equal(templateId)
                                       .asList();
    if (configFiles != null) {
      configFiles.forEach(configFile -> delete(appId, configFile.getUuid()));
    }
  }

  @Override
  public List<ConfigFile> getConfigFileByTemplate(String appId, String envId, String templateId) {
    List<ConfigFile> configFiles = wingsPersistence.createQuery(ConfigFile.class)
                                       .field("appId")
                                       .equal(appId)
                                       .field("envId")
                                       .equal(envId)
                                       .field("templateId")
                                       .equal(templateId)
                                       .asList();
    configFiles.forEach(configFile -> configFile.setOverridePath(generateOverridePath(configFile)));
    return configFiles;
  }
}
