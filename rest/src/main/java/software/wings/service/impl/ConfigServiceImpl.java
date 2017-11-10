package software.wings.service.impl;

import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Base.GLOBAL_ENV_ID;
import static software.wings.beans.EntityType.ENVIRONMENT;
import static software.wings.beans.EntityType.SERVICE;
import static software.wings.beans.ErrorCode.INVALID_ARGUMENT;
import static software.wings.beans.ErrorCode.INVALID_REQUEST;
import static software.wings.beans.SearchFilter.Builder.aSearchFilter;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.security.encryption.SimpleEncryption.CHARSET;
import static software.wings.service.intfc.FileService.FileBucket.CONFIGS;

import com.google.common.base.Preconditions;
import com.google.common.io.Files;

import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.Activity;
import software.wings.beans.ConfigFile;
import software.wings.beans.EmbeddedUser;
import software.wings.beans.EntityType;
import software.wings.beans.EntityVersion;
import software.wings.beans.EntityVersion.ChangeType;
import software.wings.beans.FeatureName;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.ServiceTemplate;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.security.EncryptionType;
import software.wings.security.UserThreadLocal;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.EncryptionUtils;
import software.wings.security.encryption.SecretChangeLog;
import software.wings.security.encryption.SecretUsageLog;
import software.wings.security.encryption.SimpleEncryption;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.EntityVersionService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.utils.BoundedInputStream;
import software.wings.utils.Validator;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.CharBuffer;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
  @Inject private HostService hostService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private ServiceTemplateService serviceTemplateService;
  @Inject private EntityVersionService entityVersionService;
  @Inject private EnvironmentService environmentService;
  @Inject private SecretManager secretManager;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private ActivityService activityService;

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
  public String save(ConfigFile configFile, BoundedInputStream inputStream) {
    String changeLogDescription = null;
    if (!StringUtils.isBlank(configFile.getUuid())) {
      changeLogDescription = "File updated";
    }

    validateEntity(configFile.getAppId(), configFile.getEntityId(), configFile.getEntityType());
    InputStream toWrite = inputStream;
    String envId = configFile.getEntityType().equals(SERVICE) || configFile.getEntityType().equals(ENVIRONMENT)
        ? GLOBAL_ENV_ID
        : serviceTemplateService.get(configFile.getAppId(), configFile.getTemplateId()).getEnvId();

    configFile.setEnvId(envId);
    configFile.setRelativeFilePath(validateAndResolveFilePath(configFile.getRelativeFilePath()));
    configFile.setDefaultVersion(1);
    if (configFile.isEncrypted()) {
      toWrite = getEncryptedInputStream(configFile, inputStream, new HashMap<>());
    }

    String fileId = fileService.saveFile(configFile, toWrite, CONFIGS);
    configFile.setSize(inputStream.getTotalBytesRead()); // set this only after saving file to gridfs
    String id = wingsPersistence.save(configFile);
    entityVersionService.newEntityVersion(configFile.getAppId(), EntityType.CONFIG, configFile.getUuid(),
        configFile.getEntityId(), configFile.getFileName(), ChangeType.CREATED, configFile.getNotes());
    if (configFile.isEncrypted()) {
      updateParentForEncryptedData(configFile);
      if (UserThreadLocal.get() != null) {
        if (StringUtils.isBlank(changeLogDescription)) {
          changeLogDescription = "File uploaded";
        }
        wingsPersistence.save(SecretChangeLog.builder()
                                  .accountId(configFile.getAccountId())
                                  .encryptedDataId(id)
                                  .description(changeLogDescription)
                                  .user(EmbeddedUser.builder()
                                            .uuid(UserThreadLocal.get().getUuid())
                                            .email(UserThreadLocal.get().getEmail())
                                            .name(UserThreadLocal.get().getName())
                                            .build())
                                  .build());
      }
    }

    fileService.updateParentEntityIdAndVersion(id, fileId, 1, CONFIGS);
    return id;
  }

  private void updateParentForEncryptedData(ConfigFile configFile) {
    if (!StringUtils.isBlank(configFile.getEncryptedFileId())) {
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
  public ConfigFile get(String appId, String configId) {
    ConfigFile configFile = wingsPersistence.get(ConfigFile.class, appId, configId);
    if (configFile == null) {
      throw new WingsException(INVALID_ARGUMENT, "args", "ConfigFile not found");
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
    return configFiles;
  }

  @Override
  public File download(String appId, String configId) {
    ConfigFile configFile = get(appId, configId);
    File file = new File(Files.createTempDir(), new File(configFile.getRelativeFilePath()).getName());
    fileService.download(configFile.getFileUuid(), file, CONFIGS);
    if (configFile.isEncrypted()) {
      file = getDecryptedFile(configFile, file, appId, null);
    }
    return file;
  }

  @Override
  public File download(String appId, String configId, Integer version) {
    ConfigFile configFile = get(appId, configId);
    int fileVersion = (version == null) ? configFile.getDefaultVersion() : version;
    String fileId = fileService.getFileIdByVersion(configId, fileVersion, CONFIGS);

    File file = new File(Files.createTempDir(), new File(configFile.getRelativeFilePath()).getName());
    fileService.download(fileId, file, CONFIGS);
    if (configFile.isEncrypted()) {
      file = getDecryptedFile(configFile, file, appId, null);
    }
    return file;
  }

  @Override
  public File downloadForActivity(String appId, String configId, String activityId) {
    ConfigFile configFile = get(appId, configId);
    File file = new File(Files.createTempDir(), new File(configFile.getRelativeFilePath()).getName());
    fileService.download(configFile.getFileUuid(), file, CONFIGS);
    if (configFile.isEncrypted()) {
      file = getDecryptedFile(configFile, file, appId, activityId);
    }
    return file;
  }

  private File getDecryptedFile(ConfigFile configFile, File file, String appId, String activityId) {
    EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, configFile.getEncryptedFileId());
    Preconditions.checkNotNull(encryptedData);
    if (!StringUtils.isBlank(activityId)) {
      Activity activity = activityService.get(activityId, appId);
      Preconditions.checkNotNull(activity, "Could not find activity " + activityId + " for app " + appId);
      SecretUsageLog secretUsageLog = SecretUsageLog.builder()
                                          .encryptedDataId(configFile.getUuid())
                                          .workflowExecutionId(activity.getWorkflowExecutionId())
                                          .envId(activity.getEnvironmentId())
                                          .accountId(encryptedData.getAccountId())
                                          .build();
      secretUsageLog.setAppId(configFile.getAppId());
      wingsPersistence.save(secretUsageLog);
    }
    return secretManager.decryptFile(file, configFile.getAccountId(), encryptedData);
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
    String oldFileId = savedConfigFile.getFileUuid();

    inputConfigFile.setEntityType(savedConfigFile.getEntityType());

    if (uploadedInputStream != null) {
      InputStream toWrite = uploadedInputStream;
      if (inputConfigFile.isEncrypted()) {
        toWrite = getEncryptedInputStream(savedConfigFile, uploadedInputStream, updateMap);
        if (UserThreadLocal.get() != null) {
          wingsPersistence.save(SecretChangeLog.builder()
                                    .accountId(inputConfigFile.getAccountId())
                                    .encryptedDataId(inputConfigFile.getUuid())
                                    .description("File updated")
                                    .user(EmbeddedUser.builder()
                                              .uuid(UserThreadLocal.get().getUuid())
                                              .email(UserThreadLocal.get().getEmail())
                                              .name(UserThreadLocal.get().getName())
                                              .build())
                                    .build());
        }
      } else if (!StringUtils.isBlank(savedConfigFile.getEncryptedFileId())) {
        wingsPersistence.delete(EncryptedData.class, savedConfigFile.getEncryptedFileId());
        updateMap.put("encryptedFileId", "");
      }
      String fileId = fileService.saveFile(inputConfigFile, toWrite, CONFIGS);
      EntityVersion entityVersion = entityVersionService.newEntityVersion(inputConfigFile.getAppId(), EntityType.CONFIG,
          inputConfigFile.getUuid(), savedConfigFile.getEntityId(), inputConfigFile.getFileName(), ChangeType.UPDATED,
          inputConfigFile.getNotes());
      fileService.updateParentEntityIdAndVersion(
          inputConfigFile.getUuid(), fileId, entityVersion.getVersion(), CONFIGS);
      if (inputConfigFile.isSetAsDefault()) {
        inputConfigFile.setDefaultVersion(entityVersion.getVersion());
      }
      updateMap.put("fileUuid", inputConfigFile.getFileUuid());
      updateMap.put("checksum", inputConfigFile.getChecksum());
      updateMap.put("size", uploadedInputStream.getTotalBytesRead());
      updateMap.put("fileName", inputConfigFile.getFileName());
      updateMap.put("encrypted", inputConfigFile.isEncrypted());
    }
    if (inputConfigFile.getDescription() != null) {
      updateMap.put("description", inputConfigFile.getDescription());
    }

    updateMap.put("defaultVersion", inputConfigFile.getDefaultVersion());

    if (inputConfigFile.getEnvIdVersionMap() != null) {
      updateMap.put("envIdVersionMap", inputConfigFile.getEnvIdVersionMap());
    }

    if (inputConfigFile.getEntityType() != SERVICE) {
      updateMap.put("configOverrideType", inputConfigFile.getConfigOverrideType());
    }

    if (inputConfigFile.getConfigOverrideExpression() != null) {
      updateMap.put("configOverrideExpression", inputConfigFile.getConfigOverrideExpression());
    }

    wingsPersistence.updateFields(ConfigFile.class, inputConfigFile.getUuid(), updateMap);
  }

  private InputStream getEncryptedInputStream(
      ConfigFile configFile, BoundedInputStream inputStream, Map<String, Object> updateMap) {
    if (!StringUtils.isBlank(configFile.getUuid())) {
      wingsPersistence.delete(EncryptedData.class, configFile.getEncryptedFileId());
    }
    String fileUuid = UUID.randomUUID().toString();
    EncryptedData encryptedData = secretManager.encryptFile(inputStream, configFile.getAccountId(), fileUuid);
    encryptedData.setUuid(fileUuid);
    char[] encryptedFileData = encryptedData.getEncryptedValue();

    // don't save encrypted data as the file is saved in gridFS
    switch (encryptedData.getEncryptionType()) {
      case VAULT:
        encryptedData.setEncryptedValue(encryptedData.getEncryptionKey().toCharArray());
        break;
      default:
        encryptedData.setEncryptedValue(null);
        break;
    }

    if (!StringUtils.isBlank(configFile.getUuid())) {
      encryptedData.addParent(configFile.getUuid());
      wingsPersistence.save(encryptedData);
    }

    configFile.setEncryptedFileId(encryptedData.getUuid());
    updateMap.put("encryptedFileId", encryptedData.getUuid());
    return new ByteArrayInputStream(CHARSET.encode(CharBuffer.wrap(encryptedFileData)).array());
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

    Query<ConfigFile> query = wingsPersistence.createQuery(ConfigFile.class)
                                  .field("appId")
                                  .equal(existingConfigFile.getAppId())
                                  .field("relativeFilePath")
                                  .equal(existingConfigFile.getRelativeFilePath());
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
                    .addFilter(aSearchFilter().build())
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

  private boolean shouldUseKms(String accountId) {
    return featureFlagService.isEnabled(FeatureName.KMS, accountId)
        && secretManager.getEncryptionType(accountId) != EncryptionType.LOCAL;
  }
}
