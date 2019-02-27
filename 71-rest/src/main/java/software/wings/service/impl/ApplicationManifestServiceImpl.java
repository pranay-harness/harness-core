package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.exception.WingsException.USER;
import static io.harness.govern.Switch.unhandled;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.Base.APP_ID_KEY;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.beans.appmanifest.ManifestFile.APPLICATION_MANIFEST_ID_KEY;
import static software.wings.beans.appmanifest.ManifestFile.FILE_NAME_KEY;
import static software.wings.beans.yaml.YamlConstants.MANIFEST_FILE_FOLDER;
import static software.wings.common.Constants.VALUES_YAML_KEY;
import static software.wings.delegatetasks.k8s.K8sTaskHelper.manifestFilesFromGitFetchFilesResult;
import static software.wings.utils.Validator.duplicateCheck;
import static software.wings.utils.Validator.notNullCheck;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.delegate.task.protocol.ResponseData;
import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.waiter.ErrorNotifyResponseData;
import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.beans.DelegateTask;
import software.wings.beans.Event.Type;
import software.wings.beans.GitFetchFilesTaskParams;
import software.wings.beans.GitFileConfig;
import software.wings.beans.Service;
import software.wings.beans.TaskType;
import software.wings.beans.appmanifest.AppManifestKind;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.ApplicationManifest.AppManifestType;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.appmanifest.StoreType;
import software.wings.beans.yaml.GitCommandExecutionResponse;
import software.wings.beans.yaml.GitCommandExecutionResponse.GitCommandStatus;
import software.wings.beans.yaml.GitFetchFilesFromMultipleRepoResult;
import software.wings.beans.yaml.GitFetchFilesResult;
import software.wings.delegatetasks.RemoteMethodReturnValueData;
import software.wings.dl.WingsPersistence;
import software.wings.helpers.ext.k8s.request.K8sValuesLocation;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.yaml.YamlDirectoryService;
import software.wings.service.intfc.yaml.YamlPushService;
import software.wings.sm.states.k8s.K8sStateHelper;
import software.wings.yaml.directory.DirectoryNode;
import software.wings.yaml.directory.DirectoryPath;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.validation.executable.ValidateOnExecution;

@ValidateOnExecution
@Singleton
public class ApplicationManifestServiceImpl implements ApplicationManifestService {
  private static final Logger logger = LoggerFactory.getLogger(ApplicationManifestServiceImpl.class);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private AppService appService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private YamlPushService yamlPushService;
  @Inject private DelegateService delegateService;
  @Inject private YamlDirectoryService yamlDirectoryService;
  @Inject private K8sStateHelper k8sStateHelper;

  @Override
  public ApplicationManifest create(ApplicationManifest applicationManifest) {
    return upsertApplicationManifest(applicationManifest, true);
  }

  @Override
  public ManifestFile createManifestFileByServiceId(ManifestFile manifestFile, String serviceId) {
    return upsertManifestFileForService(manifestFile, serviceId, true);
  }

  @Override
  public ManifestFile updateManifestFileByServiceId(ManifestFile manifestFile, String serviceId) {
    return upsertManifestFileForService(manifestFile, serviceId, false);
  }

  @Override
  public ApplicationManifest update(ApplicationManifest applicationManifest) {
    return upsertApplicationManifest(applicationManifest, false);
  }

  @Override
  public void deleteAppManifest(String appId, String appManifestId) {
    deleteAppManifest(getById(appId, appManifestId));
  }

  @Override
  public void deleteAppManifest(ApplicationManifest applicationManifest) {
    if (applicationManifest == null) {
      return;
    }

    deleteManifestFiles(applicationManifest.getAppId(), applicationManifest, applicationManifest.isSyncFromGit());

    wingsPersistence.delete(applicationManifest);

    String accountId = appService.getAccountIdByAppId(applicationManifest.getAppId());
    yamlPushService.pushYamlChangeSet(
        accountId, applicationManifest, null, Type.DELETE, applicationManifest.isSyncFromGit(), false);
  }

  @Override
  public ApplicationManifest getByServiceId(String appId, String serviceId) {
    Query<ApplicationManifest> query = wingsPersistence.createQuery(ApplicationManifest.class)
                                           .filter(ApplicationManifest.APP_ID_KEY, appId)
                                           .filter(ApplicationManifest.SERVICE_ID_KEY, serviceId)
                                           .filter(ApplicationManifest.ENV_ID_KEY, null);

    return query.get();
  }

  @Override
  public ApplicationManifest getByEnvId(String appId, String envId, AppManifestKind kind) {
    Query<ApplicationManifest> query = wingsPersistence.createQuery(ApplicationManifest.class)
                                           .filter(ApplicationManifest.APP_ID_KEY, appId)
                                           .filter(ApplicationManifest.ENV_ID_KEY, envId)
                                           .filter(ApplicationManifest.KIND_KEY, kind)
                                           .filter(ApplicationManifest.SERVICE_ID_KEY, null);
    return query.get();
  }

  @Override
  public List<ApplicationManifest> getAllByEnvId(String appId, String envId) {
    Query<ApplicationManifest> query = wingsPersistence.createQuery(ApplicationManifest.class)
                                           .filter(ApplicationManifest.APP_ID_KEY, appId)
                                           .filter(ApplicationManifest.ENV_ID_KEY, envId);
    return query.asList();
  }

  @Override
  public List<ApplicationManifest> getAllByEnvIdAndKind(String appId, String envId, AppManifestKind kind) {
    Query<ApplicationManifest> query = wingsPersistence.createQuery(ApplicationManifest.class)
                                           .filter(ApplicationManifest.APP_ID_KEY, appId)
                                           .filter(ApplicationManifest.ENV_ID_KEY, envId)
                                           .filter(ApplicationManifest.KIND_KEY, kind);
    return query.asList();
  }

  @Override
  public ApplicationManifest getByEnvAndServiceId(String appId, String envId, String serviceId, AppManifestKind kind) {
    Query<ApplicationManifest> query = wingsPersistence.createQuery(ApplicationManifest.class)
                                           .filter(ApplicationManifest.APP_ID_KEY, appId)
                                           .filter(ApplicationManifest.ENV_ID_KEY, envId)
                                           .filter(ApplicationManifest.SERVICE_ID_KEY, serviceId)
                                           .filter(ApplicationManifest.KIND_KEY, kind);
    return query.get();
  }

  @Override
  public ApplicationManifest getById(String appId, String id) {
    Query<ApplicationManifest> query = wingsPersistence.createQuery(ApplicationManifest.class)
                                           .filter(ApplicationManifest.ID_KEY, id)
                                           .filter(ApplicationManifest.APP_ID_KEY, appId);
    return query.get();
  }

  @Override
  public List<ManifestFile> getManifestFilesByAppManifestId(String appId, String applicationManifestId) {
    return wingsPersistence.createQuery(ManifestFile.class)
        .filter(APPLICATION_MANIFEST_ID_KEY, applicationManifestId)
        .filter(ManifestFile.APP_ID_KEY, appId)
        .asList();
  }

  @Override
  public ManifestFile getManifestFileById(String appId, String id) {
    Query<ManifestFile> query = wingsPersistence.createQuery(ManifestFile.class)
                                    .filter(ApplicationManifest.APP_ID_KEY, appId)
                                    .filter(ApplicationManifest.ID_KEY, id);
    return query.get();
  }

  @Override
  public ManifestFile getManifestFileByEnvId(String appId, String envId, AppManifestKind kind) {
    ApplicationManifest appManifest = getByEnvId(appId, envId, kind);
    return getManifestFileByFileName(appManifest.getUuid(), VALUES_YAML_KEY);
  }

  @Override
  public ManifestFile getManifestFileByFileName(String applicationManifestId, String fileName) {
    Query<ManifestFile> query = wingsPersistence.createQuery(ManifestFile.class)
                                    .filter(APPLICATION_MANIFEST_ID_KEY, applicationManifestId)
                                    .filter(ManifestFile.FILE_NAME_KEY, fileName);
    return query.get();
  }

  private ApplicationManifest upsertApplicationManifest(ApplicationManifest applicationManifest, boolean isCreate) {
    validateApplicationManifest(applicationManifest);
    if (!isCreate) {
      resetReadOnlyProperties(applicationManifest);
    }

    ApplicationManifest savedApplicationManifest =
        wingsPersistence.saveAndGet(ApplicationManifest.class, applicationManifest);

    String appId = savedApplicationManifest.getAppId();
    String accountId = appService.getAccountIdByAppId(appId);

    Type type = isCreate ? Type.CREATE : Type.UPDATE;
    yamlPushService.pushYamlChangeSet(accountId, isCreate ? null : savedApplicationManifest, savedApplicationManifest,
        type, applicationManifest.isSyncFromGit(), false);

    return savedApplicationManifest;
  }

  private void resetReadOnlyProperties(ApplicationManifest applicationManifest) {
    ApplicationManifest savedAppManifest = getById(applicationManifest.getAppId(), applicationManifest.getUuid());
    applicationManifest.setKind(savedAppManifest.getKind());
  }

  private void validateManifestFileName(ManifestFile manifestFile) {
    String manifestFileName = manifestFile.getFileName();

    if (isBlank(manifestFileName)) {
      throw new InvalidRequestException("Manifest file name cannot be empty", USER);
    }

    if (manifestFileName.charAt(0) == '/' || manifestFileName.charAt(manifestFileName.length() - 1) == '/') {
      throw new InvalidRequestException("Manifest file name should not begin or end with /", USER);
    }

    String[] filePathComponents = manifestFileName.split("/");
    for (String filePathComponent : filePathComponents) {
      if (isBlank(filePathComponent)) {
        throw new InvalidRequestException("Manifest file path component cannot be empty", USER);
      }

      if (!filePathComponent.trim().equals(filePathComponent)) {
        throw new InvalidRequestException(
            "Manifest file path component cannot contain leading or trailing whitespaces", USER);
      }
    }
  }

  private void validateFileNamePrefixForDirectory(ManifestFile manifestFile) {
    List<ManifestFile> manifestFiles = wingsPersistence.createQuery(ManifestFile.class)
                                           .filter(APP_ID_KEY, manifestFile.getAppId())
                                           .filter(APPLICATION_MANIFEST_ID_KEY, manifestFile.getApplicationManifestId())
                                           .project(FILE_NAME_KEY, true)
                                           .asList();

    if (isEmpty(manifestFiles)) {
      return;
    }

    Pattern pattern = Pattern.compile("^" + manifestFile.getFileName() + "/");
    boolean match = manifestFiles.stream().anyMatch(manifest -> pattern.matcher(manifest.getFileName()).find());
    if (match) {
      throw new InvalidRequestException(
          format("Cannot create manifest file with name %s. There exists a directory with same name",
              manifestFile.getFileName()),
          USER);
    }

    // Below handles case like testValidateManifestFileName4
    Set<String> manifestFilesSet = manifestFiles.stream().map(ManifestFile::getFileName).collect(Collectors.toSet());

    // To handle the edit of the existing file
    if (manifestFilesSet.contains(manifestFile.getFileName())) {
      return;
    }

    String[] filePathParts = manifestFile.getFileName().split("/");
    StringBuilder fileNamePrefixBuilder = new StringBuilder();

    for (int i = 0; i < filePathParts.length; i++) {
      fileNamePrefixBuilder.append(filePathParts[i]);
      String fileNamePrefix = fileNamePrefixBuilder.toString();

      if (manifestFilesSet.contains(fileNamePrefixBuilder.toString())) {
        throw new InvalidRequestException(
            format("Cannot create manifest file with name %s. There exists a file with path %s",
                manifestFile.getFileName(), fileNamePrefix),
            USER);
      }

      fileNamePrefixBuilder.append('/');
    }
  }

  @Override
  public ManifestFile upsertApplicationManifestFile(
      ManifestFile manifestFile, ApplicationManifest applicationManifest, boolean isCreate) {
    manifestFile.setApplicationManifestId(applicationManifest.getUuid());

    validateManifestFileName(manifestFile);
    validateFileNamePrefixForDirectory(manifestFile);
    notNullCheck("applicationManifest", applicationManifest, USER);

    ManifestFile savedManifestFile = duplicateCheck(
        () -> wingsPersistence.saveAndGet(ManifestFile.class, manifestFile), FILE_NAME_KEY, manifestFile.getFileName());

    String appId = applicationManifest.getAppId();
    String accountId = appService.getAccountIdByAppId(appId);

    Type type = isCreate ? Type.CREATE : Type.UPDATE;
    yamlPushService.pushYamlChangeSet(
        accountId, isCreate ? null : savedManifestFile, savedManifestFile, type, manifestFile.isSyncFromGit(), false);

    return savedManifestFile;
  }

  @Override
  public void pruneByService(String appId, String serviceId) {
    ApplicationManifest applicationManifest = wingsPersistence.createQuery(ApplicationManifest.class)
                                                  .filter(ApplicationManifest.APP_ID_KEY, appId)
                                                  .filter(ApplicationManifest.SERVICE_ID_KEY, serviceId)
                                                  .get();

    if (applicationManifest != null) {
      deleteAppManifest(appId, applicationManifest.getUuid());
    }
  }

  private void deleteManifestFiles(String appId, ApplicationManifest applicationManifest, boolean isSyncFromGit) {
    List<ManifestFile> manifestFiles = getManifestFilesByAppManifestId(appId, applicationManifest.getUuid());
    if (isEmpty(manifestFiles)) {
      return;
    }

    for (ManifestFile manifestFile : manifestFiles) {
      manifestFile.setSyncFromGit(isSyncFromGit);
      deleteManifestFileUtility(appId, manifestFile, applicationManifest);
    }
  }

  private void deleteManifestFileUtility(
      String appId, ManifestFile manifestFile, ApplicationManifest applicationManifest) {
    wingsPersistence.delete(manifestFile);
    String accountId = appService.getAccountIdByAppId(appId);
    yamlPushService.pushYamlChangeSet(
        accountId, applicationManifest, manifestFile, Type.DELETE, manifestFile.isSyncFromGit());
  }

  @Override
  public void deleteManifestFileById(String appId, String manifestFileId) {
    deleteManifestFile(appId, getManifestFileById(appId, manifestFileId));
  }

  @Override
  public void deleteManifestFile(String appId, ManifestFile manifestFile) {
    if (manifestFile == null) {
      return;
    }

    ApplicationManifest applicationManifest = getById(appId, manifestFile.getApplicationManifestId());
    if (applicationManifest == null) {
      return;
    }

    if (isNotBlank(applicationManifest.getEnvId())) {
      applicationManifest.setSyncFromGit(manifestFile.isSyncFromGit());
      deleteAppManifest(applicationManifest);
    } else {
      deleteManifestFileUtility(appId, manifestFile, applicationManifest);
    }
  }

  private void validateApplicationManifest(ApplicationManifest applicationManifest) {
    if (isBlank(applicationManifest.getServiceId()) && isBlank(applicationManifest.getEnvId())) {
      throw new InvalidRequestException("Both envId and serviceId cannot be empty for application manifest", USER);
    }

    GitFileConfig gitFileConfig = applicationManifest.getGitFileConfig();

    if (StoreType.Remote.equals(applicationManifest.getStoreType())) {
      notNullCheck("Git file config cannot be null for store type remote", gitFileConfig, USER);

      if (isBlank(gitFileConfig.getConnectorId())) {
        throw new InvalidRequestException("Connector id cannot be empty", USER);
      }

      if (gitFileConfig.isUseBranch() && isBlank(gitFileConfig.getBranch())) {
        throw new InvalidRequestException("Branch cannot be empty if useBranch is selected", USER);
      }

      if (!gitFileConfig.isUseBranch() && isBlank(gitFileConfig.getCommitId())) {
        throw new InvalidRequestException("CommitId cannot be empty if useBranch is not selected", USER);
      }
    } else {
      if (gitFileConfig != null) {
        throw new InvalidRequestException("Git file config should be null for store type local", USER);
      }
    }
  }

  @Override
  public DirectoryNode getManifestFilesFromGit(String appId, String appManifestId) {
    Application app = appService.get(appId);
    ApplicationManifest appManifest = getById(appId, appManifestId);
    if (appManifest == null) {
      throw new InvalidRequestException("Application manifest doesn't exist with id " + appManifestId, USER);
    }

    if (StoreType.Local.equals(appManifest.getStoreType())) {
      throw new InvalidRequestException(
          "Manifest files from git should only be requested when store type is remote", USER);
    }

    Map<K8sValuesLocation, ApplicationManifest> appManifestMap = new HashMap<>();
    appManifestMap.put(K8sValuesLocation.Service, appManifest);

    GitFetchFilesTaskParams fetchFilesTaskParams =
        k8sStateHelper.createGitFetchFilesTaskParams(null, app, appManifestMap);
    fetchFilesTaskParams.setActivityId(generateUuid());

    DelegateTask delegateTask = aDelegateTask()
                                    .withAccountId(app.getAccountId())
                                    .withAppId(app.getUuid())
                                    .withAsync(false)
                                    .withTaskType(TaskType.GIT_FETCH_FILES_TASK.name())
                                    .withParameters(new Object[] {fetchFilesTaskParams})
                                    .withTimeout(TimeUnit.MINUTES.toMillis(60))
                                    .build();

    ResponseData notifyResponseData;
    try {
      notifyResponseData = delegateService.executeTask(delegateTask);
    } catch (InterruptedException ex) {
      throw new InvalidRequestException(ex.getMessage(), WingsException.USER);
    }

    if (notifyResponseData instanceof ErrorNotifyResponseData) {
      throw new WingsException(((ErrorNotifyResponseData) notifyResponseData).getErrorMessage());
    } else if ((notifyResponseData instanceof RemoteMethodReturnValueData)
        && (((RemoteMethodReturnValueData) notifyResponseData).getException() instanceof InvalidRequestException)) {
      throw(InvalidRequestException)((RemoteMethodReturnValueData) notifyResponseData).getException();
    } else if (!(notifyResponseData instanceof GitCommandExecutionResponse)) {
      throw new WingsException(ErrorCode.GENERAL_ERROR)
          .addParam("message", "Unknown Response from delegate")
          .addContext(ResponseData.class, notifyResponseData);
    }

    GitCommandExecutionResponse executionResponse = (GitCommandExecutionResponse) notifyResponseData;
    if (!executionResponse.getGitCommandStatus().equals(GitCommandStatus.SUCCESS)) {
      throw new InvalidRequestException(executionResponse.getErrorMessage());
    }

    String prefixPath = fetchFilesTaskParams.getGitFetchFilesConfigMap()
                            .get(K8sValuesLocation.Service.name())
                            .getGitFileConfig()
                            .getFilePath();
    GitFetchFilesFromMultipleRepoResult filesFromMultipleGitResult =
        (GitFetchFilesFromMultipleRepoResult) executionResponse.getGitCommandResult();
    GitFetchFilesResult gitFetchFilesResult =
        filesFromMultipleGitResult.getFilesFromMultipleRepo().get(K8sValuesLocation.Service.name());

    List<ManifestFile> manifestFiles = manifestFilesFromGitFetchFilesResult(gitFetchFilesResult, prefixPath);

    Service service = serviceResourceService.get(appId, appManifest.getServiceId());
    return yamlDirectoryService.generateManifestFileFolderNode(
        app.getAccountId(), service, manifestFiles, new DirectoryPath(MANIFEST_FILE_FOLDER));
  }

  private ManifestFile upsertManifestFileForService(ManifestFile manifestFile, String serviceId, boolean isCreate) {
    if (!serviceResourceService.exist(manifestFile.getAppId(), serviceId)) {
      throw new InvalidRequestException("Service doesn't exist");
    }

    return upsertApplicationManifestFile(manifestFile, getByServiceId(manifestFile.getAppId(), serviceId), isCreate);
  }

  @Override
  public ApplicationManifest getAppManifest(String appId, String envId, String serviceId, AppManifestKind kind) {
    AppManifestType appManifestType = getAppManifestType(envId, serviceId);

    switch (appManifestType) {
      case SERVICE:
        return getByServiceId(appId, serviceId);
      case ENV:
        return getByEnvId(appId, envId, kind);
      case ENV_SERVICE:
        return getByEnvAndServiceId(appId, envId, serviceId, kind);
      default:
        unhandled(appManifestType);
        throw new WingsException("Invalid application manifest type");
    }
  }

  @Override
  public AppManifestType getAppManifestType(ApplicationManifest applicationManifest) {
    String serviceId = applicationManifest.getServiceId();
    String envId = applicationManifest.getEnvId();

    return getAppManifestType(envId, serviceId);
  }

  private AppManifestType getAppManifestType(String envId, String serviceId) {
    if (isNotBlank(envId) && isNotBlank(serviceId)) {
      return AppManifestType.ENV_SERVICE;
    } else if (isNotBlank(envId)) {
      return AppManifestType.ENV;
    } else if (isNotBlank(serviceId)) {
      return AppManifestType.SERVICE;
    } else {
      throw new WingsException("App manifest is invalid with empty envId and serviceId");
    }
  }

  @Override
  public void pruneByEnvironment(String appId, String envId) {
    List<ApplicationManifest> appManifests = getAllByEnvId(appId, envId);

    if (isEmpty(appManifests)) {
      return;
    }

    for (ApplicationManifest applicationManifest : appManifests) {
      deleteAppManifest(appId, applicationManifest.getUuid());
    }
  }
}
