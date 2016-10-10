package software.wings.service.impl;

import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Base.GLOBAL_ENV_ID;
import static software.wings.beans.EntityType.SERVICE;
import static software.wings.beans.ErrorCodes.INVALID_ARGUMENT;
import static software.wings.beans.ErrorCodes.INVALID_REQUEST;
import static software.wings.beans.ErrorCodes.UNKNOWN_ERROR;
import static software.wings.beans.SearchFilter.Builder.aSearchFilter;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.service.intfc.FileService.FileBucket.CONFIGS;

import com.google.common.io.Files;

import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.ConfigFile;
import software.wings.beans.EntityType;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.infrastructure.ApplicationHost;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.TagService;
import software.wings.utils.Validator;

import java.io.File;
import java.io.InputStream;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
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
  @Inject private ServiceTemplateService serviceTemplateService;

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ConfigService#list(software.wings.dl.PageRequest)
   */
  @Override
  public PageResponse<ConfigFile> list(PageRequest<ConfigFile> request) {
    PageResponse<ConfigFile> pageResponse = wingsPersistence.query(ConfigFile.class, request);
    pageResponse.getResponse().parallelStream().forEach(
        configFile -> configFile.setVersions(fileService.getAllFileIds(configFile.getUuid(), CONFIGS)));
    return pageResponse;
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ConfigService#save(software.wings.beans.ConfigFile, java.io.InputStream)
   */
  @Override
  public String save(ConfigFile configFile, InputStream inputStream) {
    validateEntity(configFile.getAppId(), configFile.getEntityId(), configFile.getEntityType());
    String envId = configFile.getEntityType().equals(SERVICE)
        ? GLOBAL_ENV_ID
        : serviceTemplateService.get(configFile.getAppId(), configFile.getTemplateId()).getEnvId();

    configFile.setEnvId(envId);
    configFile.setRelativeFilePath(validateAndResolveFilePath(configFile.getRelativeFilePath()));
    String fileId = fileService.saveFile(configFile, inputStream, CONFIGS);
    String id = wingsPersistence.save(configFile);
    fileService.updateParentEntityId(id, fileId, CONFIGS);
    return id;
  }

  private void validateEntity(String appId, String entityId, EntityType entityType) {
    boolean entityExist;
    if (EntityType.SERVICE.equals(entityType)) {
      entityExist = serviceResourceService.exist(appId, entityId);
    } else if (EntityType.TAG.equals(entityType) || EntityType.ENVIRONMENT.equals(entityType)) {
      entityExist = tagService.exist(appId, entityId);
    } else if (EntityType.HOST.equals(entityType)) {
      entityExist = hostService.exist(appId, entityId);
    } else {
      throw new WingsException(INVALID_ARGUMENT, "args", "Config upload not supported for entityType " + entityType);
    }
    if (!entityExist) {
      throw new WingsException(INVALID_REQUEST, "message", "Node identifier and node type do not match");
    }
  }

  @Override
  public String validateAndResolveFilePath(String relativePath) {
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

    configFile.setVersions(fileService.getAllFileIds(configId, CONFIGS));

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
    File file = new File(Files.createTempDir(), new File(configFile.getRelativeFilePath()).getName());
    fileService.download(configFile.getFileUuid(), file, CONFIGS);
    return file;
  }

  @Override
  public File download(String appId, String configId, String version) {
    ConfigFile configFile = get(appId, configId, false);
    File file = new File(Files.createTempDir(), new File(configFile.getRelativeFilePath()).getName());
    String fileId = configFile.getFileUuid();
    if (configFile.getVersions().contains(version)) {
      fileId = version;
    }
    fileService.download(fileId, file, CONFIGS);
    return file;
  }

  private String generateOverridePath(ConfigFile configFile) {
    switch (configFile.getEntityType()) {
      case SERVICE:
        return serviceResourceService.get(configFile.getAppId(), configFile.getEntityId()).getName();
      case TAG:
      case ENVIRONMENT:
        return tagService.getTagHierarchyPathString(
            tagService.get(configFile.getAppId(), configFile.getEnvId(), configFile.getEntityId(), true));
      case HOST:
        ApplicationHost host = hostService.get(configFile.getAppId(), configFile.getEnvId(), configFile.getEntityId());
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

    if (savedConfigFile.getEntityType().equals(SERVICE)
        && !savedConfigFile.getRelativeFilePath().equals(inputConfigFile.getRelativeFilePath())) {
      updateRelativeFilePathForServiceAndAllOverrideFiles(savedConfigFile, inputConfigFile.getRelativeFilePath());
    }

    Map<String, Object> updateMap = new HashMap<>();
    String oldFileId = savedConfigFile.getFileUuid();

    if (uploadedInputStream != null) {
      String fileId = fileService.saveFile(inputConfigFile, uploadedInputStream, CONFIGS);
      fileService.updateParentEntityId(inputConfigFile.getUuid(), fileId, CONFIGS);
      updateMap.put("fileUuid", inputConfigFile.getFileUuid());
      updateMap.put("checksum", inputConfigFile.getChecksum());
      updateMap.put("size", inputConfigFile.getSize());
      updateMap.put("fileName", inputConfigFile.getFileName());
    }
    if (inputConfigFile.getDescription() != null) {
      updateMap.put("description", inputConfigFile.getDescription());
    }
    wingsPersistence.updateFields(ConfigFile.class, inputConfigFile.getUuid(), updateMap);

    if (!oldFileId.equals(inputConfigFile.getFileUuid())) { // new file updated successfully delete old file gridfs file
      executorService.submit(() -> fileService.deleteFile(oldFileId, CONFIGS));
    }
  }

  /**
   * Update relative file path for service and all override files.
   *
   * @param existingConfigFile  the existing config file
   * @param newRelativeFilePath the new relative file path
   */
  public void updateRelativeFilePathForServiceAndAllOverrideFiles(
      ConfigFile existingConfigFile, String newRelativeFilePath) {
    String resolvedFilePath = validateAndResolveFilePath(newRelativeFilePath);

    List<Object> templateIds =
        serviceTemplateService
            .getTemplateRefKeysByService(existingConfigFile.getAppId(), existingConfigFile.getEntityId(), null)
            .stream()
            .map(serviceTemplateKey -> serviceTemplateKey.getId())
            .collect(Collectors.toList());

    Query<ConfigFile> query =
        wingsPersistence.createQuery(ConfigFile.class).field("appId").equal(existingConfigFile.getAppId());
    query.or(query.criteria("entityId").equal(existingConfigFile.getEntityId()),
        query.criteria("templateId").in(templateIds));

    UpdateOperations<ConfigFile> updateOperations =
        wingsPersistence.createUpdateOperations(ConfigFile.class).set("relativeFilePath", resolvedFilePath);
    wingsPersistence.update(query, updateOperations);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ConfigService#delete(java.lang.String)
   */
  @Override
  public void delete(String appId, String configId) {
    boolean deleted = wingsPersistence.delete(
        wingsPersistence.createQuery(ConfigFile.class).field("appId").equal(appId).field(ID_KEY).equal(configId));
    if (deleted) {
      List<ConfigFile> configFiles = wingsPersistence.createQuery(ConfigFile.class)
                                         .field("appId")
                                         .equal(appId)
                                         .field("parentConfigFileId")
                                         .equal(configId)
                                         .asList();
      if (configFiles.size() != 0) {
        configFiles.forEach(configFile -> delete(appId, configFile.getUuid()));
      }
      executorService.submit(() -> fileService.deleteAllFilesForEntity(configId, CONFIGS));
    }
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ConfigService#getConfigFilesForEntity(java.lang.String, java.lang.String)
   */
  @Override
  public List<ConfigFile> getConfigFilesForEntity(String appId, String templateId, String entityId) {
    return list(aPageRequest()
                    .addFilter(aSearchFilter().withField("appId", Operator.EQ, appId).build())
                    .addFilter(aSearchFilter().withField("templateId", Operator.EQ, templateId).build())
                    .addFilter(aSearchFilter().withField("entityId", Operator.EQ, entityId).build())
                    .build())
        .getResponse();
  }

  @Override
  public void deleteByEntityId(String appId, String templateId, String entityId) {
    List<ConfigFile> configFiles = getConfigFilesForEntity(appId, templateId, entityId);
    if (configFiles != null) {
      configFiles.forEach(configFile -> delete(appId, configFile.getUuid()));
    }
  }

  @Override
  public void deleteByTemplateId(String appId, String serviceTemplateId) {
    wingsPersistence.createQuery(ConfigFile.class)
        .field("appId")
        .equal(appId)
        .field("templateId")
        .equal(serviceTemplateId)
        .asList()
        .forEach(configFile -> delete(appId, configFile.getUuid()));
  }

  @Override
  public void deleteByEntityId(String appId, String entityId) {
    wingsPersistence.createQuery(ConfigFile.class)
        .field("appId")
        .equal(appId)
        .field("entityId")
        .equal(entityId)
        .asList()
        .forEach(configFile -> delete(appId, configFile.getUuid()));
  }
}
