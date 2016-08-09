package software.wings.service.impl;

import static software.wings.beans.ErrorCodes.INVALID_ARGUMENT;
import static software.wings.beans.ErrorCodes.UNKNOWN_ERROR;
import static software.wings.service.intfc.FileService.FileBucket.CONFIGS;
import static software.wings.utils.FileUtils.createTempDirPath;

import software.wings.beans.ConfigFile;
import software.wings.beans.EntityType;
import software.wings.beans.Host;
import software.wings.beans.ServiceTemplate;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.TagService;
import software.wings.utils.Validator;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by anubhaw on 4/25/16.
 */
@ValidateOnExecution
@Singleton
public class ConfigServiceImpl implements ConfigService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private FileService fileService;
  @Inject private TagService tagService;
  @Inject private HostService hostService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject ExecutorService executorService;

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
    if (Arrays.asList(EntityType.SERVICE, EntityType.TAG, EntityType.HOST).contains(configFile.getEntityType())) {
      fileService.saveFile(configFile, inputStream, CONFIGS);
      return wingsPersistence.save(configFile);
    } else {
      throw new WingsException(
          INVALID_ARGUMENT, "args", "Config upload not supported for entityType " + configFile.getEntityType());
    }
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ConfigService#get(java.lang.String)
   */
  @Override
  public ConfigFile get(String appId, String configId, boolean withOverridePath) {
    ConfigFile configFile = wingsPersistence.get(ConfigFile.class, appId, configId);
    if (configFile == null) {
      throw new WingsException(INVALID_ARGUMENT, "message", "ConfigFile not found");
    }

    if (withOverridePath) {
      configFile.setOverridePath(generateOverridePath(configFile));
    }
    return configFile;
  }

  @Override
  public List<ConfigFile> getConfigFileByTemplate(String appId, String envId, ServiceTemplate serviceTemplate) {
    List<ConfigFile> configFiles = wingsPersistence.createQuery(ConfigFile.class)
                                       .field("appId")
                                       .equal(appId)
                                       .field("envId")
                                       .equal(envId)
                                       .field("templateId")
                                       .equal(serviceTemplate.getUuid())
                                       .asList();
    configFiles.forEach(configFile -> configFile.setOverridePath(generateOverridePath(configFile)));
    return configFiles;
  }

  @Override
  public File download(String appId, String configId) {
    ConfigFile configFile = get(appId, configId, false);
    File file = new File(createTempDirPath(), configFile.getName());
    fileService.download(configFile.getFileUuid(), file, CONFIGS);
    return file;
  }

  private String generateOverridePath(ConfigFile configFile) {
    switch (configFile.getEntityType()) {
      case SERVICE:
        return serviceResourceService.get(configFile.getAppId(), configFile.getEntityId()).getName();
      case TAG:
        return tagService.getTagHierarchyPathString(
            tagService.get(configFile.getAppId(), configFile.getEnvId(), configFile.getEntityId()));
      case HOST:
        Host host = hostService.getHostByEnv(configFile.getAppId(), configFile.getEnvId(), configFile.getEntityId());
        String tagHierarchyPathString = tagService.getTagHierarchyPathString(host.getConfigTag());
        return tagHierarchyPathString + "/" + host.getHostName();
      default:
        throw new WingsException(UNKNOWN_ERROR, "message", "Unknown entity type encountered");
    }
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ConfigService#update(software.wings.beans.ConfigFile, java.io.InputStream)
   */
  @Override
  public void update(ConfigFile configFile, InputStream uploadedInputStream) {
    ConfigFile savedConfigFile = get(configFile.getAppId(), configFile.getUuid(), false);
    Validator.notNullCheck("Configuration file", savedConfigFile);

    Map<String, Object> updateMap = new HashMap<>();
    String oldFileId = savedConfigFile.getFileUuid();

    if (uploadedInputStream != null) {
      fileService.saveFile(configFile, uploadedInputStream, CONFIGS);
      updateMap.put("fileUuid", configFile.getFileUuid());
      updateMap.put("fileName", configFile.getFileName());
      updateMap.put("checksum", configFile.getChecksum());
      updateMap.put("size", configFile.getSize());
    }
    updateMap.put("name", configFile.getName());
    updateMap.put("relativePath", configFile.getRelativePath());
    wingsPersistence.updateFields(ConfigFile.class, configFile.getUuid(), updateMap);

    if (!oldFileId.equals(configFile.getFileUuid())) { // new file updated successfully delete old file gridfs file
      executorService.submit(() -> fileService.deleteFile(oldFileId, CONFIGS));
    }
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
}
