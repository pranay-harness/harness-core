package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Base.GLOBAL_ENV_ID;
import static software.wings.beans.ConfigFile.DEFAULT_TEMPLATE_ID;
import static software.wings.beans.EntityType.ENVIRONMENT;
import static software.wings.beans.EntityType.SERVICE;
import static software.wings.beans.ErrorCode.INVALID_ARGUMENT;
import static software.wings.beans.yaml.Change.ChangeType.ADD;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.service.intfc.FileService.FileBucket.CONFIGS;

import com.google.common.base.Preconditions;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.Activity;
import software.wings.beans.ConfigFile;
import software.wings.beans.EntityType;
import software.wings.beans.EntityVersion;
import software.wings.beans.EntityVersion.ChangeType;
import software.wings.beans.SearchFilter;
import software.wings.beans.SearchFilter.Operator;
import software.wings.dl.PageRequest;
import software.wings.dl.PageRequest.PageRequestBuilder;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.InvalidRequestException;
import software.wings.exception.WingsException;
import software.wings.security.EncryptionType;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.SecretUsageLog;
import software.wings.service.impl.yaml.YamlChangeSetHelper;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.EntityVersionService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.FileService.FileBucket;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.utils.BoundedInputStream;
import software.wings.utils.Validator;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
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
  @Inject private HostService hostService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private ServiceTemplateService serviceTemplateService;
  @Inject private EntityVersionService entityVersionService;
  @Inject private EnvironmentService environmentService;
  @Inject private SecretManager secretManager;
  @Inject private ActivityService activityService;
  @Inject private YamlChangeSetHelper yamlChangeSetHelper;

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ConfigService#list(software.wings.dl.PageRequest)
   */
  @Override
  public PageResponse<ConfigFile> list(PageRequest<ConfigFile> request) {
    PageResponse<ConfigFile> response = wingsPersistence.query(ConfigFile.class, request);
    response.getResponse().forEach(configFile -> {
      if (configFile.isEncrypted()) {
        EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, configFile.getEncryptedFileId());
        Preconditions.checkNotNull(encryptedData, "No encrypted record found for " + configFile.getUuid());
        configFile.setSecretFileName(encryptedData.getName());
      }
    });
    return response;
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ConfigService#save(software.wings.beans.ConfigFile, java.io.InputStream)
   */
  @Override
  public String save(ConfigFile configFile, BoundedInputStream inputStream) {
    validateEntity(configFile.getAppId(), configFile.getEntityId(), configFile.getEntityType());
    String envId = configFile.getEntityType().equals(SERVICE) || configFile.getEntityType().equals(ENVIRONMENT)
        ? GLOBAL_ENV_ID
        : serviceTemplateService.get(configFile.getAppId(), configFile.getTemplateId()).getEnvId();

    configFile.setEnvId(envId);
    configFile.setRelativeFilePath(validateAndResolveFilePath(configFile.getRelativeFilePath()));
    configFile.setDefaultVersion(1);
    String fileId;
    if (configFile.isEncrypted()) {
      EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, configFile.getEncryptedFileId());
      Preconditions.checkNotNull(encryptedData, "No encrypted record found " + configFile);
      fileId = String.valueOf(encryptedData.getEncryptedValue());
      configFile.setSize(encryptedData.getFileSize());
    } else {
      fileId = fileService.saveFile(configFile, inputStream, CONFIGS);
      configFile.setSize(inputStream.getTotalBytesRead()); // set this only after saving file to gridfs
    }

    String id = wingsPersistence.save(configFile);
    entityVersionService.newEntityVersion(configFile.getAppId(), EntityType.CONFIG, configFile.getUuid(),
        configFile.getEntityId(), configFile.getFileName(), ChangeType.CREATED, configFile.getNotes());

    if (secretManager.getEncryptionType(configFile.getAccountId()) != EncryptionType.VAULT) {
      fileService.updateParentEntityIdAndVersion(null, id, 1, fileId, null, CONFIGS);
    }

    if (configFile.isEncrypted()) {
      updateParentForEncryptedData(configFile);
    }

    ConfigFile configFileFromDB = get(configFile.getAppId(), id);
    yamlChangeSetHelper.configFileYamlChangeAsync(configFileFromDB, ADD);
    return id;
  }

  private void updateParentForEncryptedData(ConfigFile configFile) {
    if (isNotBlank(configFile.getEncryptedFileId())) {
      EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, configFile.getEncryptedFileId());
      encryptedData.addParent(configFile.getUuid());
      wingsPersistence.save(encryptedData);
    }
  }

  private void validateEntity(String appId, String entityId, EntityType entityType) {
    boolean entityExist;
    if (EntityType.SERVICE.equals(entityType)) {
      entityExist = serviceResourceService.exist(appId, entityId);
    } else if (EntityType.ENVIRONMENT.equals(entityType)) {
      entityExist = environmentService.exist(appId, entityId);
    } else if (EntityType.HOST.equals(entityType)) {
      entityExist = hostService.exist(appId, entityId);
    } else if (EntityType.SERVICE_TEMPLATE.equals(entityType)) {
      entityExist = serviceTemplateService.exist(appId, entityId);
    } else {
      throw new WingsException(INVALID_ARGUMENT)
          .addParam("args", "Config upload not supported for entityType " + entityType);
    }
    if (!entityExist) {
      throw new InvalidRequestException("Node identifier and node type do not match");
    }
  }

  @Override
  public String validateAndResolveFilePath(String relativePath) {
    try {
      Path path = Paths.get(relativePath);
      if (path.isAbsolute()) {
        throw new WingsException(INVALID_ARGUMENT).addParam("args", "Relative path can not be absolute");
      }
      return path.normalize().toString();
    } catch (InvalidPathException ex) {
      throw new WingsException(INVALID_ARGUMENT).addParam("args", "Invalid relativePath");
    }
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ConfigService#get(java.lang.String)
   */
  @Override
  public ConfigFile get(String appId, String configId) {
    ConfigFile configFile = wingsPersistence.get(ConfigFile.class, appId, configId);
    if (configFile == null) {
      throw new WingsException(INVALID_ARGUMENT).addParam("args", "ConfigFile not found");
    }
    return configFile;
  }

  @Override
  public ConfigFile get(String appId, String entityId, EntityType entityType, String relativeFilePath) {
    PageRequestBuilder builder = aPageRequest();
    String columnName;
    if (EntityType.SERVICE.equals(entityType)) {
      columnName = "entityId";
      builder.addFilter("entityType", Operator.EQ, entityType.name());
    } else if (EntityType.ENVIRONMENT.equals(entityType)) {
      columnName = "envId";
    } else {
      return null;
    }

    builder.addFilter(columnName, Operator.EQ, entityId);
    builder.addFilter("appId", Operator.EQ, appId);
    builder.addFilter("relativeFilePath", Operator.EQ, relativeFilePath);

    return wingsPersistence.get(ConfigFile.class, builder.build());
  }

  @Override
  public List<ConfigFile> getConfigFileByTemplate(String appId, String envId, String serviceTemplateId) {
    return wingsPersistence.createQuery(ConfigFile.class)
        .filter("appId", appId)
        .filter("envId", envId)
        .filter("templateId", serviceTemplateId)
        .asList();
  }

  @Override
  public File download(String appId, String configId) {
    ConfigFile configFile = get(appId, configId);
    File file = new File(Files.createTempDir(), new File(configFile.getRelativeFilePath()).getName());
    if (configFile.isEncrypted()) {
      file = getDecryptedFile(configFile, file, appId, null);
    } else {
      fileService.download(configFile.getFileUuid(), file, CONFIGS);
    }
    return file;
  }

  @Override
  public File download(String appId, String configId, Integer version) {
    ConfigFile configFile = get(appId, configId);
    int fileVersion = (version == null) ? configFile.getDefaultVersion() : version;
    String fileId = fileService.getFileIdByVersion(configId, fileVersion, CONFIGS);

    // if default version is already set 0, fileId may be null or not latest.
    if (isEmpty(fileId) || !fileService.getLatestFileId(configId, CONFIGS).equals(fileId)) {
      fileId = configFile.getFileUuid();
    }

    File file = new File(Files.createTempDir(), new File(configFile.getRelativeFilePath()).getName());
    if (configFile.isEncrypted()) {
      file = getDecryptedFile(configFile, file, appId, null);
    } else {
      fileService.download(fileId, file, CONFIGS);
    }
    return file;
  }

  @Override
  public File downloadForActivity(String appId, String configId, String activityId) {
    ConfigFile configFile = get(appId, configId);
    File file = new File(Files.createTempDir(), new File(configFile.getRelativeFilePath()).getName());
    if (configFile.isEncrypted()) {
      file = getDecryptedFile(configFile, file, appId, activityId);
    } else {
      fileService.download(configFile.getFileUuid(), file, CONFIGS);
    }
    return file;
  }

  @SuppressFBWarnings("DM_DEFAULT_ENCODING")
  @Override
  public String getFileContent(String appId, ConfigFile configFile) {
    if (configFile.isEncrypted()) {
      return secretManager.getFileContents(configFile.getAccountId(), configFile.getEncryptedFileId());
    } else {
      OutputStream outputStream = new ByteArrayOutputStream();
      InputStream inputStream = fileService.openDownloadStream(configFile.getFileUuid(), FileBucket.CONFIGS);
      try {
        outputStream.write(ByteStreams.toByteArray(inputStream));
        outputStream.flush();
      } catch (IOException e) {
        throw new WingsException(INVALID_ARGUMENT, e)
            .addParam("args", "Failed to get configFile content: " + configFile.getName());
      }
      return outputStream.toString();
    }
  }

  private File getDecryptedFile(ConfigFile configFile, File file, String appId, String activityId) {
    EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, configFile.getEncryptedFileId());
    Preconditions.checkNotNull(encryptedData);
    if (isNotBlank(activityId)) {
      Activity activity = activityService.get(activityId, appId);
      Preconditions.checkNotNull(activity, "Could not find activity " + activityId + " for app " + appId);
      SecretUsageLog secretUsageLog = SecretUsageLog.builder()
                                          .encryptedDataId(encryptedData.getUuid())
                                          .workflowExecutionId(activity.getWorkflowExecutionId())
                                          .envId(activity.getEnvironmentId())
                                          .accountId(encryptedData.getAccountId())
                                          .build();
      secretUsageLog.setAppId(configFile.getAppId());
      wingsPersistence.save(secretUsageLog);
    }
    return secretManager.getFile(encryptedData.getAccountId(), configFile.getEncryptedFileId(), file);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ConfigService#update(software.wings.beans.ConfigFile, java.io.InputStream)
   */
  @Override
  public void update(ConfigFile inputConfigFile, BoundedInputStream uploadedInputStream) {
    ConfigFile savedConfigFile = get(inputConfigFile.getAppId(), inputConfigFile.getUuid());
    Validator.notNullCheck("Configuration File", savedConfigFile);

    if (savedConfigFile.getEntityType().equals(SERVICE)
        && !savedConfigFile.getRelativeFilePath().equals(inputConfigFile.getRelativeFilePath())) {
      updateRelativeFilePathForServiceAndAllOverrideFiles(savedConfigFile, inputConfigFile.getRelativeFilePath());
    }

    Map<String, Object> updateMap = new HashMap<>();

    inputConfigFile.setEntityType(savedConfigFile.getEntityType());
    String fileId = null;
    if (inputConfigFile.isEncrypted()) {
      EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, inputConfigFile.getEncryptedFileId());
      Preconditions.checkNotNull(encryptedData, "No encrypted record found " + inputConfigFile);
      fileId = String.valueOf(encryptedData.getEncryptedValue());
      updateMap.put("encryptedFileId", inputConfigFile.getEncryptedFileId());
      updateMap.put("size", encryptedData.getFileSize());
      updateParentForEncryptedData(inputConfigFile);
    } else if (uploadedInputStream != null) {
      fileId = fileService.saveFile(inputConfigFile, uploadedInputStream, CONFIGS);
      updateMap.put("encryptedFileId", "");

      updateMap.put("fileUuid", inputConfigFile.getFileUuid());
      updateMap.put("checksum", inputConfigFile.getChecksum());
      updateMap.put("size", uploadedInputStream.getTotalBytesRead());
      updateMap.put("fileName", inputConfigFile.getFileName());
    }
    updateMap.put("encrypted", inputConfigFile.isEncrypted());

    if (isNotBlank(fileId)) {
      EntityVersion entityVersion = entityVersionService.newEntityVersion(inputConfigFile.getAppId(), EntityType.CONFIG,
          inputConfigFile.getUuid(), savedConfigFile.getEntityId(), inputConfigFile.getFileName(), ChangeType.UPDATED,
          inputConfigFile.getNotes());
      if (inputConfigFile.isSetAsDefault()) {
        inputConfigFile.setDefaultVersion(entityVersion.getVersion());
      }

      if (secretManager.getEncryptionType(inputConfigFile.getAccountId()) != EncryptionType.VAULT) {
        fileService.updateParentEntityIdAndVersion(
            null, inputConfigFile.getUuid(), entityVersion.getVersion(), fileId, null, CONFIGS);
      }
    }
    if (inputConfigFile.getDescription() != null) {
      updateMap.put("description", inputConfigFile.getDescription());
    }

    if (inputConfigFile.getEnvIdVersionMap() != null) {
      updateMap.put("envIdVersionMap", inputConfigFile.getEnvIdVersionMap());
    }

    if (inputConfigFile.getEntityType() != SERVICE) {
      updateMap.put("configOverrideType", inputConfigFile.getConfigOverrideType());
    }

    updateMap.put("defaultVersion", savedConfigFile.getDefaultVersion() + 1);

    if (inputConfigFile.getConfigOverrideExpression() != null) {
      updateMap.put("configOverrideExpression", inputConfigFile.getConfigOverrideExpression());
    }

    wingsPersistence.updateFields(ConfigFile.class, inputConfigFile.getUuid(), updateMap);
  }

  /**
   * Update relative file path for service and all override files.
   *
   * @param existingConfigFile  the existing config file
   * @param newRelativeFilePath the new relative file path
   */
  private void updateRelativeFilePathForServiceAndAllOverrideFiles(
      ConfigFile existingConfigFile, String newRelativeFilePath) {
    String resolvedFilePath = validateAndResolveFilePath(newRelativeFilePath);

    List<Object> templateIds =
        serviceTemplateService
            .getTemplateRefKeysByService(existingConfigFile.getAppId(), existingConfigFile.getEntityId(), null)
            .stream()
            .map(Key::getId)
            .collect(toList());

    Query<ConfigFile> query = wingsPersistence.createQuery(ConfigFile.class)
                                  .filter("appId", existingConfigFile.getAppId())
                                  .filter("relativeFilePath", existingConfigFile.getRelativeFilePath());
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
    // TODO: migrate to prune pattern

    Query<ConfigFile> query =
        wingsPersistence.createQuery(ConfigFile.class).filter(ConfigFile.APP_ID_KEY, appId).filter(ID_KEY, configId);
    ConfigFile configFile = query.get();
    if (configFile == null) {
      return;
    }

    boolean deleted = wingsPersistence.delete(query);
    if (deleted) {
      if (configFile.isEncrypted()) {
        EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, configFile.getEncryptedFileId());
        if (encryptedData != null) {
          encryptedData.removeParentId(configFile.getUuid());
          wingsPersistence.save(encryptedData);
        }
      }

      List<ConfigFile> configFiles = wingsPersistence.createQuery(ConfigFile.class)
                                         .filter(ConfigFile.APP_ID_KEY, appId)
                                         .filter("parentConfigFileId", configId)
                                         .asList();
      if (!configFiles.isEmpty()) {
        configFiles.forEach(childConfigFile -> delete(appId, childConfigFile.getUuid()));
      }
      if (!configFile.isEncrypted()) {
        executorService.submit(() -> fileService.deleteAllFilesForEntity(configId, CONFIGS));
      }
    }
  }

  @Override
  public void delete(String appId, String entityId, EntityType entityType, String configFileName) {
    PageRequest<ConfigFile> pageRequest = aPageRequest()
                                              .addFilter("appId", Operator.EQ, appId)
                                              .addFilter("entityType", Operator.EQ, entityType.name())
                                              .addFilter("entityId", Operator.EQ, entityId)
                                              .addFilter("relativeFilePath", Operator.EQ, configFileName)
                                              .build();

    ConfigFile configFile = wingsPersistence.get(ConfigFile.class, pageRequest);
    boolean deleted = wingsPersistence.delete(ConfigFile.class, configFile.getUuid());
    if (deleted) {
      List<ConfigFile> childConfigFiles = wingsPersistence.createQuery(ConfigFile.class)
                                              .filter("appId", appId)
                                              .filter("parentConfigFileId", configFile.getUuid())
                                              .asList();
      childConfigFiles.forEach(childConfigFile -> delete(appId, childConfigFile.getUuid()));

      executorService.submit(() -> fileService.deleteAllFilesForEntity(configFile.getUuid(), CONFIGS));
    }
  }

  @Override
  public List<ConfigFile> getConfigFilesForEntity(String appId, String templateId, String entityId) {
    return list(aPageRequest()
                    .addFilter("appId", Operator.EQ, appId)
                    .addFilter("templateId", Operator.EQ, templateId)
                    .addFilter("entityId", Operator.EQ, entityId)
                    .build())
        .getResponse();
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ConfigService#getConfigFilesForEntity(java.lang.String, java.lang.String)
   */
  @Override
  public List<ConfigFile> getConfigFilesForEntity(String appId, String templateId, String entityId, String envId) {
    return list(aPageRequest()
                    .addFilter("appId", Operator.EQ, appId)
                    .addFilter("templateId", Operator.EQ, templateId)
                    .addFilter("entityId", Operator.EQ, entityId)
                    .addFilter(SearchFilter.builder().build())
                    .build())
        .getResponse();
  }

  @Override
  public List<ConfigFile> getConfigFileOverridesForEnv(String appId, String envId) {
    // All service overrides
    List allServiceOverrideList = list(aPageRequest()
                                           .addFilter("appId", Operator.EQ, appId)
                                           .addFilter("entityType", Operator.EQ, EntityType.ENVIRONMENT)
                                           .addFilter("entityId", Operator.EQ, envId)
                                           .addFilter(SearchFilter.builder().build())
                                           .build())
                                      .getResponse();

    // service override
    List overrideList = list(aPageRequest()
                                 .addFilter("appId", Operator.EQ, appId)
                                 .addFilter("entityType", Operator.EQ, EntityType.SERVICE_TEMPLATE)
                                 .addFilter("envId", Operator.EQ, envId)
                                 .addFilter(SearchFilter.builder().build())
                                 .build())
                            .getResponse();

    overrideList.addAll(allServiceOverrideList);
    return overrideList;
  }

  @Override
  public void deleteByEntityId(String appId, String templateId, String entityId) {
    List<ConfigFile> configFiles = getConfigFilesForEntity(appId, templateId, entityId);
    if (configFiles != null) {
      configFiles.forEach(configFile -> delete(appId, configFile.getUuid()));
    }
  }

  @Override
  public void pruneByService(String appId, String entityId) {
    List<ConfigFile> configFiles = getConfigFilesForEntity(appId, DEFAULT_TEMPLATE_ID, entityId);
    if (configFiles != null) {
      configFiles.forEach(configFile -> delete(appId, configFile.getUuid()));
    }
  }

  @Override
  public void deleteByTemplateId(String appId, String serviceTemplateId) {
    wingsPersistence.createQuery(ConfigFile.class)
        .filter("appId", appId)
        .filter("templateId", serviceTemplateId)
        .asList()
        .forEach(configFile -> delete(appId, configFile.getUuid()));
  }

  @Override
  public void deleteByEntityId(String appId, String entityId) {
    wingsPersistence.createQuery(ConfigFile.class)
        .filter("appId", appId)
        .filter("entityId", entityId)
        .asList()
        .forEach(configFile -> delete(appId, configFile.getUuid()));
  }

  @Override
  public void pruneByHost(String appId, String hostId) {
    deleteByEntityId(appId, hostId);
  }
}
