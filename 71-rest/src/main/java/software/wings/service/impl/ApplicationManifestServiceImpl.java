package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.exception.WingsException.USER;
import static io.harness.govern.Switch.unhandled;
import static io.harness.validation.PersistenceValidator.duplicateCheck;
import static io.harness.validation.Validator.notNullCheck;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.appmanifest.AppManifestKind.HELM_CHART_OVERRIDE;
import static software.wings.beans.appmanifest.ManifestFile.VALUES_YAML_KEY;
import static software.wings.beans.appmanifest.StoreType.HelmChartRepo;
import static software.wings.beans.appmanifest.StoreType.HelmSourceRepo;
import static software.wings.beans.yaml.YamlConstants.MANIFEST_FILE_FOLDER;
import static software.wings.delegatetasks.GitFetchFilesTask.GIT_FETCH_FILES_TASK_ASYNC_TIMEOUT;
import static software.wings.delegatetasks.k8s.K8sTaskHelper.manifestFilesFromGitFetchFilesResult;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.DelegateTask;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import software.wings.api.DeploymentType;
import software.wings.beans.Application;
import software.wings.beans.Application.ApplicationKeys;
import software.wings.beans.Event.Type;
import software.wings.beans.GitFetchFilesTaskParams;
import software.wings.beans.GitFileConfig;
import software.wings.beans.HelmChartConfig;
import software.wings.beans.Service;
import software.wings.beans.TaskType;
import software.wings.beans.appmanifest.AppManifestKind;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.ApplicationManifest.AppManifestSource;
import software.wings.beans.appmanifest.ApplicationManifest.ApplicationManifestKeys;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.appmanifest.ManifestFile.ManifestFileKeys;
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
import software.wings.utils.ApplicationManifestUtils;
import software.wings.yaml.directory.DirectoryNode;
import software.wings.yaml.directory.DirectoryPath;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
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
@Slf4j
public class ApplicationManifestServiceImpl implements ApplicationManifestService {
  private static final int ALLOWED_SIZE_IN_BYTES = 1024 * 1024; // 1 MiB

  @Inject private WingsPersistence wingsPersistence;
  @Inject private AppService appService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private YamlPushService yamlPushService;
  @Inject private DelegateService delegateService;
  @Inject private YamlDirectoryService yamlDirectoryService;
  @Inject private ApplicationManifestUtils applicationManifestUtils;

  private static long MAX_MANIFEST_FILES_PER_APPLICATION_MANIFEST = 50L;

  @Override
  public ApplicationManifest create(ApplicationManifest applicationManifest) {
    return upsertApplicationManifest(applicationManifest, true);
  }

  @Override
  public ManifestFile createManifestFileByServiceId(ManifestFile manifestFile, String serviceId) {
    return createManifestFileByServiceId(manifestFile, serviceId, false);
  }

  @Override
  public ManifestFile createManifestFileByServiceId(
      ManifestFile manifestFile, String serviceId, boolean removeNamespace) {
    doFileValidations(manifestFile);
    if (removeNamespace) {
      removeNamespace(manifestFile);
    }
    return upsertManifestFileForService(manifestFile, serviceId, true);
  }

  @Override
  public ManifestFile updateManifestFileByServiceId(ManifestFile manifestFile, String serviceId) {
    return updateManifestFileByServiceId(manifestFile, serviceId, false);
  }

  @Override
  public ManifestFile updateManifestFileByServiceId(
      ManifestFile manifestFile, String serviceId, boolean removeNamespace) {
    doFileValidations(manifestFile);
    if (removeNamespace) {
      removeNamespace(manifestFile);
    }
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
  public ApplicationManifest getManifestByServiceId(String appId, String serviceId) {
    Query<ApplicationManifest> query = wingsPersistence.createQuery(ApplicationManifest.class)
                                           .filter(ApplicationKeys.appId, appId)
                                           .filter(ApplicationManifestKeys.serviceId, serviceId)
                                           .filter(ApplicationManifestKeys.envId, null)
                                           .filter(ApplicationManifestKeys.kind, AppManifestKind.K8S_MANIFEST);

    return query.get();
  }

  @Override
  public ApplicationManifest getByEnvId(String appId, String envId, AppManifestKind kind) {
    Query<ApplicationManifest> query = wingsPersistence.createQuery(ApplicationManifest.class)
                                           .filter(ApplicationKeys.appId, appId)
                                           .filter(ApplicationManifestKeys.envId, envId)
                                           .filter(ApplicationManifestKeys.kind, kind)
                                           .filter(ApplicationManifestKeys.serviceId, null);
    return query.get();
  }

  @Override
  public List<ApplicationManifest> getAllByEnvId(String appId, String envId) {
    Query<ApplicationManifest> query = wingsPersistence.createQuery(ApplicationManifest.class)
                                           .filter(ApplicationKeys.appId, appId)
                                           .filter(ApplicationManifestKeys.envId, envId);
    return query.asList();
  }

  @Override
  public List<ApplicationManifest> getAllByEnvIdAndKind(String appId, String envId, AppManifestKind kind) {
    Query<ApplicationManifest> query = wingsPersistence.createQuery(ApplicationManifest.class)
                                           .filter(ApplicationKeys.appId, appId)
                                           .filter(ApplicationManifestKeys.envId, envId)
                                           .filter(ApplicationManifestKeys.kind, kind);
    return query.asList();
  }

  @Override
  public ApplicationManifest getByEnvAndServiceId(String appId, String envId, String serviceId, AppManifestKind kind) {
    Query<ApplicationManifest> query = wingsPersistence.createQuery(ApplicationManifest.class)
                                           .filter(ApplicationKeys.appId, appId)
                                           .filter(ApplicationManifestKeys.envId, envId)
                                           .filter(ApplicationManifestKeys.serviceId, serviceId)
                                           .filter(ApplicationManifestKeys.kind, kind);
    return query.get();
  }

  @Override
  public ApplicationManifest getById(String appId, String id) {
    Query<ApplicationManifest> query = wingsPersistence.createQuery(ApplicationManifest.class)
                                           .filter(ApplicationManifest.ID_KEY, id)
                                           .filter(ApplicationKeys.appId, appId);
    return query.get();
  }

  @Override
  public List<ManifestFile> getManifestFilesByAppManifestId(String appId, String applicationManifestId) {
    return wingsPersistence.createQuery(ManifestFile.class)
        .filter(ManifestFileKeys.applicationManifestId, applicationManifestId)
        .filter(ApplicationKeys.appId, appId)
        .asList();
  }

  @Override
  public ManifestFile getManifestFileById(String appId, String id) {
    Query<ManifestFile> query = wingsPersistence.createQuery(ManifestFile.class)
                                    .filter(ApplicationKeys.appId, appId)
                                    .filter(ApplicationManifest.ID_KEY, id);
    return query.get();
  }

  @Override
  public ManifestFile getManifestFileByEnvId(String appId, String envId, AppManifestKind kind) {
    ApplicationManifest appManifest = getByEnvId(appId, envId, kind);
    return getManifestFileByFileName(appManifest.getUuid(), VALUES_YAML_KEY);
  }

  @Override
  public List<ManifestFile> getOverrideManifestFilesByEnvId(String appId, String envId) {
    Query<ApplicationManifest> query = wingsPersistence.createQuery(ApplicationManifest.class)
                                           .filter(ApplicationKeys.appId, appId)
                                           .filter(ApplicationManifestKeys.envId, envId)
                                           .filter(ApplicationManifestKeys.serviceId, null);
    List<ApplicationManifest> applicationManifests = query.asList();

    List<ManifestFile> manifestFiles = new ArrayList<>();
    for (ApplicationManifest applicationManifest : applicationManifests) {
      List<ManifestFile> manifestFilesByAppManifestId =
          getManifestFilesByAppManifestId(applicationManifest.getAppId(), applicationManifest.getUuid());

      if (isNotEmpty(manifestFilesByAppManifestId)) {
        manifestFiles.addAll(manifestFilesByAppManifestId);
      }
    }

    return manifestFiles;
  }

  @Override
  public List<ApplicationManifest> listAppManifests(String appId, String serviceId) {
    Query<ApplicationManifest> query = wingsPersistence.createQuery(ApplicationManifest.class)
                                           .filter(ApplicationKeys.appId, appId)
                                           .filter(ApplicationManifestKeys.serviceId, serviceId)
                                           .filter(ApplicationManifestKeys.envId, null);
    return query.asList();
  }

  @Override
  public List<ManifestFile> listManifestFiles(String appManifestId, String appId) {
    Query<ManifestFile> query = wingsPersistence.createQuery(ManifestFile.class)
                                    .filter(ApplicationKeys.appId, appId)
                                    .filter(ManifestFileKeys.applicationManifestId, appManifestId);
    return query.asList();
  }

  @Override
  public ManifestFile getManifestFileByFileName(String applicationManifestId, String fileName) {
    Query<ManifestFile> query = wingsPersistence.createQuery(ManifestFile.class)
                                    .filter(ManifestFileKeys.applicationManifestId, applicationManifestId)
                                    .filter(ManifestFileKeys.fileName, fileName);
    return query.get();
  }

  private ApplicationManifest upsertApplicationManifest(ApplicationManifest applicationManifest, boolean isCreate) {
    validateApplicationManifest(applicationManifest);
    sanitizeApplicationManifestConfigs(applicationManifest);

    if (isCreate && exists(applicationManifest)) {
      StringBuilder builder = new StringBuilder();
      builder.append("App Manifest already exists for app ")
          .append(applicationManifest.getAppId())
          .append(" with kind ")
          .append(applicationManifest.getKind());

      if (isNotBlank(applicationManifest.getServiceId())) {
        builder.append(", serviceId ").append(applicationManifest.getServiceId());
      }

      if (isNotBlank(applicationManifest.getEnvId())) {
        builder.append(", envId ").append(applicationManifest.getEnvId());
      }

      throw new InvalidRequestException(builder.toString(), USER);
    }
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

  @VisibleForTesting
  boolean exists(ApplicationManifest applicationManifest) {
    ApplicationManifest appManifest = getAppManifest(applicationManifest.getAppId(), applicationManifest.getEnvId(),
        applicationManifest.getServiceId(), applicationManifest.getKind());

    return appManifest != null;
  }

  @VisibleForTesting
  void resetReadOnlyProperties(ApplicationManifest applicationManifest) {
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

      // We dont allow this as it gets saved outside manifest files folder in git
      if (filePathComponent.equals("..")) {
        throw new InvalidRequestException("Manifest file path component cannot contain [..]", USER);
      }
    }
  }

  private void validateFileNamePrefixForDirectory(ManifestFile manifestFile) {
    List<ManifestFile> manifestFiles =
        wingsPersistence.createQuery(ManifestFile.class)
            .filter(ApplicationKeys.appId, manifestFile.getAppId())
            .filter(ManifestFileKeys.applicationManifestId, manifestFile.getApplicationManifestId())
            .project(ManifestFileKeys.fileName, true)
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
    manifestFile.setAccountId(appService.getAccountIdByAppId(manifestFile.getAppId()));

    validateManifestFileName(manifestFile);
    validateFileNamePrefixForDirectory(manifestFile);
    notNullCheck("applicationManifest", applicationManifest, USER);

    if (isCreate
        && getManifestFilesCount(applicationManifest.getUuid()) >= MAX_MANIFEST_FILES_PER_APPLICATION_MANIFEST) {
      throw new InvalidRequestException("Cannot add more manifest files. Maximum manifest files supported are "
          + MAX_MANIFEST_FILES_PER_APPLICATION_MANIFEST);
    }

    ManifestFile savedManifestFile =
        duplicateCheck(()
                           -> wingsPersistence.saveAndGet(ManifestFile.class, manifestFile),
            ManifestFileKeys.fileName, manifestFile.getFileName());

    String appId = applicationManifest.getAppId();
    String accountId = appService.getAccountIdByAppId(appId);

    Type type = isCreate ? Type.CREATE : Type.UPDATE;
    yamlPushService.pushYamlChangeSet(
        accountId, isCreate ? null : savedManifestFile, savedManifestFile, type, manifestFile.isSyncFromGit(), false);

    return savedManifestFile;
  }

  @Override
  public void pruneByService(String appId, String serviceId) {
    List<ApplicationManifest> applicationManifests = wingsPersistence.createQuery(ApplicationManifest.class)
                                                         .filter(ApplicationKeys.appId, appId)
                                                         .filter(ApplicationManifestKeys.serviceId, serviceId)
                                                         .asList();
    for (ApplicationManifest appManifest : applicationManifests) {
      deleteAppManifest(appId, appManifest.getUuid());
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

    if (isNotBlank(applicationManifest.getEnvId()) || applicationManifest.getKind() == AppManifestKind.VALUES) {
      applicationManifest.setSyncFromGit(manifestFile.isSyncFromGit());
      deleteAppManifest(applicationManifest);
    } else {
      deleteManifestFileUtility(appId, manifestFile, applicationManifest);
    }
  }

  @VisibleForTesting
  void validateAppManifestForEnvironment(ApplicationManifest appManifest) {
    if (isNotBlank(appManifest.getEnvId())) {
      if (HELM_CHART_OVERRIDE == appManifest.getKind()) {
        validateStoreTypeForHelmChartOverride(appManifest.getStoreType());
      } else {
        if (StoreType.Local != appManifest.getStoreType() && StoreType.Remote != appManifest.getStoreType()) {
          throw new InvalidRequestException(
              "Only local and remote store types are allowed for values.yaml in environment");
        }
      }
    }
  }

  void validateStoreTypeForHelmChartOverride(StoreType storeType) {
    if (HelmChartRepo != storeType && HelmSourceRepo != storeType) {
      throw new InvalidRequestException(
          "Only HelmChartRepo and HelmSourceRepo store types are allowed for Helm Chart Override in environment");
    }
  }

  @VisibleForTesting
  void validateHelmChartRepoAppManifest(ApplicationManifest applicationManifest) {
    if (applicationManifest.getGitFileConfig() != null) {
      throw new InvalidRequestException(
          "gitFileConfig cannot be used with HelmChartRepo. Use helmChartConfig instead.", USER);
    }

    if (shouldValidateHelmChartRepoAppManifestForK8sv2(applicationManifest)) {
      validateHelmChartRepoAppManifestForK8sv2Service(applicationManifest);
      return;
    }

    validateHelmChartRepoAppManifestForHelmService(applicationManifest);
  }

  private boolean shouldValidateHelmChartRepoAppManifestForK8sv2(ApplicationManifest applicationManifest) {
    if (HELM_CHART_OVERRIDE == applicationManifest.getKind()) {
      return true;
    }

    Service service =
        serviceResourceService.get(applicationManifest.getAppId(), applicationManifest.getServiceId(), false);
    return service.isK8sV2();
  }

  private void validateHelmChartRepoAppManifestForHelmService(ApplicationManifest applicationManifest) {
    HelmChartConfig helmChartConfig = applicationManifest.getHelmChartConfig();
    if (helmChartConfig == null) {
      return;
    }

    if (isNotBlank(helmChartConfig.getConnectorId())) {
      if (isBlank(helmChartConfig.getChartName())) {
        throw new InvalidRequestException("Chart name cannot be empty when helm repository is selected", USER);
      }

      if (isNotBlank(helmChartConfig.getChartUrl())) {
        throw new InvalidRequestException("Chart url cannot be used when helm repository is selected", USER);
      }
    }
  }

  private void validateHelmChartRepoAppManifestForK8sv2Service(ApplicationManifest applicationManifest) {
    HelmChartConfig helmChartConfig = applicationManifest.getHelmChartConfig();

    notNullCheck("helmChartConfig has to be specified for HelmChartRepo.", helmChartConfig, USER);

    if (isBlank(helmChartConfig.getConnectorId())) {
      throw new InvalidRequestException("Helm repository cannot be empty.", USER);
    }

    if (isBlank(helmChartConfig.getChartName())) {
      throw new InvalidRequestException("Chart name cannot be empty.", USER);
    }

    if (isNotBlank(helmChartConfig.getChartUrl())) {
      throw new InvalidRequestException("Chart url cannot be used.", USER);
    }
  }

  @VisibleForTesting
  public void validateLocalAppManifest(ApplicationManifest applicationManifest) {
    if (applicationManifest.getGitFileConfig() != null) {
      throw new InvalidRequestException("gitFileConfig cannot be used for Local storeType.", USER);
    }

    if (applicationManifest.getHelmChartConfig() != null) {
      throw new InvalidRequestException("helmChartConfig cannot be used for Local storeType.", USER);
    }

    if (applicationManifest.getKind() != null && applicationManifest.getKind() == AppManifestKind.K8S_MANIFEST
        && applicationManifest.getServiceId() != null) {
      Service service =
          serviceResourceService.getWithDetails(applicationManifest.getAppId(), applicationManifest.getServiceId());
      if (service != null && service.getDeploymentType() != null
          && service.getDeploymentType() == DeploymentType.HELM) {
        throw new InvalidRequestException("Local app manifest is not supported for helm service", USER);
      }
    }
  }

  private void validateRemoteAppManifest(ApplicationManifest applicationManifest) {
    if (applicationManifest.getHelmChartConfig() != null) {
      throw new InvalidRequestException("helmChartConfig cannot be used with Remote. Use gitFileConfig instead.", USER);
    }
    validateGitFileConfigForRemoteManifest(applicationManifest.getGitFileConfig());
  }

  private void validateKustomizeAppManifest(ApplicationManifest applicationManifest) {
    if (applicationManifest.getKustomizeConfig() == null) {
      throw new InvalidRequestException("KustomizeConfig must be present for Kustomize Manifests", USER);
    }
    GitFileConfig gitFileConfig = applicationManifest.getGitFileConfig();
    validateGitFileConfigForRemoteManifest(gitFileConfig);
    if (isNotEmpty(gitFileConfig.getFilePath())) {
      throw new InvalidRequestException("File Path has to be empty for Git Config for Kustomize Manifests", USER);
    }
  }

  private void validateGitFileConfigForRemoteManifest(GitFileConfig gitFileConfig) {
    notNullCheck("gitFileConfig has to be specified for Remote", gitFileConfig, USER);
    if (isBlank(gitFileConfig.getConnectorId())) {
      throw new InvalidRequestException("Connector id cannot be empty. ", USER);
    }

    if (gitFileConfig.isUseBranch() && isBlank(gitFileConfig.getBranch())) {
      throw new InvalidRequestException("Branch cannot be empty if useBranch is selected.", USER);
    }

    if (!gitFileConfig.isUseBranch() && isBlank(gitFileConfig.getCommitId())) {
      throw new InvalidRequestException("CommitId cannot be empty if useBranch is not selected.", USER);
    }
  }

  @VisibleForTesting
  void validateApplicationManifest(ApplicationManifest applicationManifest) {
    if (isBlank(applicationManifest.getServiceId()) && isBlank(applicationManifest.getEnvId())) {
      throw new InvalidRequestException("Both envId and serviceId cannot be empty for application manifest", USER);
    }

    if (applicationManifest.getKind() == null) {
      throw new InvalidRequestException("Application manifest kind cannot be empty", USER);
    }

    validateAppManifestForEnvironment(applicationManifest);

    switch (applicationManifest.getStoreType()) {
      case Remote:
      case HelmSourceRepo:
        validateRemoteAppManifest(applicationManifest);
        break;
      case KustomizeSourceRepo:
        validateKustomizeAppManifest(applicationManifest);
        break;

      case Local:
        validateLocalAppManifest(applicationManifest);
        break;

      case HelmChartRepo:
        validateHelmChartRepoAppManifest(applicationManifest);
        break;

      default:
        unhandled(applicationManifest.getStoreType());
    }

    // Special validation for HELM_CHART_OVERRIDE
    if (HELM_CHART_OVERRIDE == applicationManifest.getKind() && isBlank(applicationManifest.getServiceId())) {
      throw new InvalidRequestException("Override For all Services is not Applicable for HELM_CHART_OVERRIDE", USER);
    }
  }

  @VisibleForTesting
  void sanitizeApplicationManifestConfigs(ApplicationManifest applicationManifest) {
    if (isEmpty(applicationManifest.getAccountId())) {
      applicationManifest.setAccountId(appService.getAccountIdByAppId(applicationManifest.getAppId()));
    }
    switch (applicationManifest.getStoreType()) {
      case Local:
      case Remote:
      case HelmSourceRepo:
        break;
      case KustomizeSourceRepo:
        applicationManifest.getKustomizeConfig().setKustomizeDirPath(
            defaultString(applicationManifest.getKustomizeConfig().getKustomizeDirPath()));
        break;

      case HelmChartRepo:
        HelmChartConfig helmChartConfig = applicationManifest.getHelmChartConfig();

        if (helmChartConfig == null) {
          return;
        }

        if (helmChartConfig.getConnectorId() != null) {
          helmChartConfig.setConnectorId(helmChartConfig.getConnectorId().trim());
        }

        if (helmChartConfig.getChartName() != null) {
          helmChartConfig.setChartName(helmChartConfig.getChartName().trim());
        }

        if (helmChartConfig.getChartVersion() != null) {
          helmChartConfig.setChartVersion(helmChartConfig.getChartVersion().trim());
        }

        if (helmChartConfig.getChartUrl() != null) {
          helmChartConfig.setChartUrl(helmChartConfig.getChartUrl().trim());
        }

        break;

      default:
        unhandled(applicationManifest.getStoreType());
    }
  }

  @Override
  public DirectoryNode getManifestFilesFromGit(String appId, String appManifestId) {
    Application app = appService.get(appId);
    ApplicationManifest appManifest = getById(appId, appManifestId);
    if (appManifest == null) {
      throw new InvalidRequestException("Application manifest doesn't exist with id " + appManifestId, USER);
    }

    if (StoreType.Remote != appManifest.getStoreType()) {
      throw new InvalidRequestException(
          "Manifest files from git should only be requested when store type is remote", USER);
    }

    Map<K8sValuesLocation, ApplicationManifest> appManifestMap = new HashMap<>();
    appManifestMap.put(K8sValuesLocation.Service, appManifest);

    GitFetchFilesTaskParams fetchFilesTaskParams =
        applicationManifestUtils.createGitFetchFilesTaskParams(null, app, appManifestMap);
    fetchFilesTaskParams.setActivityId(generateUuid());
    fetchFilesTaskParams.setAppManifestKind(AppManifestKind.K8S_MANIFEST);

    DelegateTask delegateTask = DelegateTask.builder()
                                    .accountId(app.getAccountId())
                                    .appId(app.getUuid())
                                    .async(false)
                                    .data(TaskData.builder()
                                              .taskType(TaskType.GIT_FETCH_FILES_TASK.name())
                                              .parameters(new Object[] {fetchFilesTaskParams})
                                              .timeout(TimeUnit.MINUTES.toMillis(GIT_FETCH_FILES_TASK_ASYNC_TIMEOUT))
                                              .build())
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
    if (executionResponse.getGitCommandStatus() != GitCommandStatus.SUCCESS) {
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

    Service service = serviceResourceService.getWithDetails(appId, appManifest.getServiceId());
    return yamlDirectoryService.generateManifestFileFolderNode(
        app.getAccountId(), service, manifestFiles, new DirectoryPath(MANIFEST_FILE_FOLDER));
  }

  private ManifestFile upsertManifestFileForService(ManifestFile manifestFile, String serviceId, boolean isCreate) {
    if (!serviceResourceService.exist(manifestFile.getAppId(), serviceId)) {
      throw new InvalidRequestException("Service doesn't exist");
    }

    return upsertApplicationManifestFile(
        manifestFile, getManifestByServiceId(manifestFile.getAppId(), serviceId), isCreate);
  }

  @Override
  public ApplicationManifest getAppManifest(String appId, String envId, String serviceId, AppManifestKind kind) {
    AppManifestSource appManifestSource = getAppManifestSource(envId, serviceId);

    switch (appManifestSource) {
      case SERVICE:
        return getByServiceId(appId, serviceId, kind);
      case ENV:
        return getByEnvId(appId, envId, kind);
      case ENV_SERVICE:
        return getByEnvAndServiceId(appId, envId, serviceId, kind);
      default:
        unhandled(appManifestSource);
        throw new WingsException("Invalid application manifest type");
    }
  }

  @Override
  public ApplicationManifest getByServiceId(String appId, String serviceId, AppManifestKind kind) {
    Query<ApplicationManifest> query = wingsPersistence.createQuery(ApplicationManifest.class)
                                           .filter(ApplicationKeys.appId, appId)
                                           .filter(ApplicationManifestKeys.serviceId, serviceId)
                                           .filter(ApplicationManifestKeys.envId, null)
                                           .filter(ApplicationManifestKeys.kind, kind);

    return query.get();
  }

  @Override
  public void cloneManifestFiles(
      String appId, ApplicationManifest applicationManifestOld, ApplicationManifest applicationManifestNew) {
    List<ManifestFile> manifestFiles = getManifestFilesByAppManifestId(appId, applicationManifestOld.getUuid());

    if (isEmpty(manifestFiles)) {
      return;
    }

    for (ManifestFile manifestFile : manifestFiles) {
      ManifestFile manifestFileNew = manifestFile.cloneInternal();
      upsertApplicationManifestFile(manifestFileNew, applicationManifestNew, true);
    }
  }

  @VisibleForTesting
  public void removeNamespace(ManifestFile manifestFile) {
    if (manifestFile == null) {
      return;
    }
    String fileContent = manifestFile.getFileContent();
    if (isEmpty(fileContent)) {
      return;
    }
    String[] fileContentByLine = fileContent.split("\\r?\\n");

    StringBuilder builder = new StringBuilder(fileContent.length());

    boolean inMetadataSection = false;
    boolean firstLine = true;

    for (String line : fileContentByLine) {
      boolean append = true;

      if (line.startsWith("metadata:")) {
        inMetadataSection = true;
      } else if (inMetadataSection && line.charAt(0) != ' ') {
        inMetadataSection = false;
      } else if (inMetadataSection && line.startsWith("  namespace:")) {
        append = false;
      }

      if (append) {
        if (!firstLine) {
          builder.append(System.lineSeparator());
        }
        builder.append(line);
      }

      firstLine = false;
    }
    manifestFile.setFileContent(builder.toString());
  }

  @Override
  public AppManifestSource getAppManifestType(ApplicationManifest applicationManifest) {
    String serviceId = applicationManifest.getServiceId();
    String envId = applicationManifest.getEnvId();

    return getAppManifestSource(envId, serviceId);
  }

  private AppManifestSource getAppManifestSource(String envId, String serviceId) {
    if (isNotBlank(envId) && isNotBlank(serviceId)) {
      return AppManifestSource.ENV_SERVICE;
    } else if (isNotBlank(envId)) {
      return AppManifestSource.ENV;
    } else if (isNotBlank(serviceId)) {
      return AppManifestSource.SERVICE;
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

  private void doFileValidations(ManifestFile manifestFile) {
    doFileSizeValidation(manifestFile, ALLOWED_SIZE_IN_BYTES);
  }

  @VisibleForTesting
  public void doFileSizeValidation(ManifestFile manifestFile, int allowedSizeInBytes) {
    if (isEmpty(manifestFile.getFileContent())) {
      return;
    }
    byte[] bytes;
    try {
      bytes = manifestFile.getFileContent().getBytes("UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new InvalidRequestException("Unable to read manifest file", e, USER);
    }
    if (bytes.length > allowedSizeInBytes) {
      throw new InvalidRequestException(format("File size: %s bytes exceeded the limit", bytes.length), USER);
    }
  }

  private long getManifestFilesCount(String appManifestId) {
    return wingsPersistence.createQuery(ManifestFile.class)
        .filter(ManifestFileKeys.applicationManifestId, appManifestId)
        .count();
  }

  @Override
  public void deleteAllManifestFilesByAppManifestId(String appId, String appManifestId) {
    ApplicationManifest applicationManifest = getById(appId, appManifestId);

    if (applicationManifest == null) {
      return;
    }

    deleteManifestFiles(applicationManifest.getAppId(), applicationManifest, false);
  }

  @Override
  public PageResponse<ApplicationManifest> list(PageRequest<ApplicationManifest> pageRequest) {
    return wingsPersistence.query(ApplicationManifest.class, pageRequest);
  }
}
