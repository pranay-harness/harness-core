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
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
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
  /**
   * The Executor service.
   */
  @Inject ExecutorService executorService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private FileService fileService;
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
    if (Arrays.asList(EntityType.SERVICE, EntityType.TAG, EntityType.HOST).contains(configFile.getEntityType())) {
      configFile.setRelativeFilePath(
          validateAndResolveFilePath(configFile.getRelativeFilePath(), configFile.getFileName()));
      fileService.saveFile(configFile, inputStream, CONFIGS);
      return wingsPersistence.save(configFile);
    } else {
      throw new WingsException(
          INVALID_ARGUMENT, "args", "Config upload not supported for entityType " + configFile.getEntityType());
    }
  }

  @Override
  public String validateAndResolveFilePath(String relativePath, String fileName) {
    if (!relativePath.endsWith(fileName)) {
      throw new WingsException(INVALID_ARGUMENT, "args", "Relative file path doesn't end with uploaded file name");
    }

    try {
      Path path = Paths.get(relativePath);
      if (path.isAbsolute()) {
        throw new WingsException(INVALID_ARGUMENT, "args", "Relative path can not be absolute");
      }
      return path.normalize().toString();
    } catch (InvalidPathException | NullPointerException ex) {
      throw new WingsException(INVALID_ARGUMENT, "args", "Invalid relativePath");
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
    File file = new File(createTempDirPath(), new File(configFile.getRelativeFilePath()).getName());
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
  public void update(ConfigFile inputConfigFile, InputStream uploadedInputStream) {
    ConfigFile savedConfigFile = get(inputConfigFile.getAppId(), inputConfigFile.getUuid(), false);
    Validator.notNullCheck("Configuration file", savedConfigFile);

    Map<String, Object> updateMap = new HashMap<>();
    String oldFileId = savedConfigFile.getFileUuid();

    if (uploadedInputStream != null) {
      fileService.saveFile(inputConfigFile, uploadedInputStream, CONFIGS);
      updateMap.put("fileUuid", inputConfigFile.getFileUuid());
      updateMap.put("checksum", inputConfigFile.getChecksum());
      updateMap.put("size", inputConfigFile.getSize());
    }
    updateMap.put("name", inputConfigFile.getName());
    wingsPersistence.updateFields(ConfigFile.class, inputConfigFile.getUuid(), updateMap);

    if (!oldFileId.equals(inputConfigFile.getFileUuid())) { // new file updated successfully delete old file gridfs file
      executorService.submit(() -> fileService.deleteFile(oldFileId, CONFIGS));
    }
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ConfigService#delete(java.lang.String)
   */
  @Override
  public void delete(String appId, String configId) {
    ConfigFile configFile = get(appId, configId, false);
    wingsPersistence.delete(configFile);
    executorService.submit(() -> fileService.deleteFile(configFile.getFileUuid(), CONFIGS));
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
    List<ConfigFile> configFiles = getConfigFilesForEntity(appId, templateId, entityId);
    if (configFiles != null) {
      configFiles.forEach(configFile -> delete(appId, configFile.getUuid()));
    }
  }
}
