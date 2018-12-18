package software.wings.service.impl;

import static io.harness.exception.WingsException.USER;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.beans.appmanifest.ManifestFile.APP_MANIFEST_FILE_NAME;
import static software.wings.beans.yaml.YamlConstants.MANIFEST_FILE_FOLDER;
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
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.appmanifest.StoreType;
import software.wings.beans.yaml.GitCommandExecutionResponse;
import software.wings.beans.yaml.GitCommandExecutionResponse.GitCommandStatus;
import software.wings.beans.yaml.GitFetchFilesResult;
import software.wings.delegatetasks.RemoteMethodReturnValueData;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.yaml.YamlDirectoryService;
import software.wings.service.intfc.yaml.YamlPushService;
import software.wings.sm.states.k8s.K8sStateHelper;
import software.wings.yaml.directory.DirectoryNode;
import software.wings.yaml.directory.DirectoryPath;

import java.util.List;
import java.util.concurrent.TimeUnit;
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
  public ManifestFile createManifestFile(ManifestFile manifestFile, String serviceId) {
    return upsertApplicationManifestFile(manifestFile, serviceId, true);
  }

  @Override
  public ManifestFile updateManifestFile(ManifestFile manifestFile, String serviceId) {
    return upsertApplicationManifestFile(manifestFile, serviceId, false);
  }

  @Override
  public ApplicationManifest update(ApplicationManifest applicationManifest) {
    return upsertApplicationManifest(applicationManifest, false);
  }

  @Override
  public void deleteAppManifest(String appId, String appManifestId) {
    Query<ApplicationManifest> query = wingsPersistence.createQuery(ApplicationManifest.class)
                                           .filter(ApplicationManifest.APP_ID_KEY, appId)
                                           .filter(ApplicationManifest.ID_KEY, appManifestId);
    wingsPersistence.delete(query);
  }

  @Override
  public ApplicationManifest get(String appId, String serviceId) {
    Query<ApplicationManifest> query = wingsPersistence.createQuery(ApplicationManifest.class)
                                           .filter(ApplicationManifest.APP_ID_KEY, appId)
                                           .filter(ApplicationManifest.SERVICE_ID_KEY, serviceId);
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
  public List<ManifestFile> getManifestFiles(String appId, String serviceId) {
    Query<ApplicationManifest> query = wingsPersistence.createQuery(ApplicationManifest.class)
                                           .filter(ApplicationManifest.APP_ID_KEY, appId)
                                           .filter(ApplicationManifest.SERVICE_ID_KEY, serviceId);
    ApplicationManifest applicationManifest = query.get();

    return getManifestFilesByAppManifestId(appId, applicationManifest.getUuid());
  }

  @Override
  public List<ManifestFile> getManifestFilesByAppManifestId(String appId, String applicationManifestId) {
    return wingsPersistence.createQuery(ManifestFile.class)
        .filter(ManifestFile.APP_MANIFEST_ID_KEY, applicationManifestId)
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
  public ManifestFile getManifestFileByFileName(String applicationManifestId, String fileName) {
    Query<ManifestFile> query = wingsPersistence.createQuery(ManifestFile.class)
                                    .filter(ManifestFile.APP_MANIFEST_ID_KEY, applicationManifestId)
                                    .filter(APP_MANIFEST_FILE_NAME, fileName);
    return query.get();
  }

  private ApplicationManifest upsertApplicationManifest(ApplicationManifest applicationManifest, boolean isCreate) {
    validateApplicationManifest(applicationManifest);

    if (!serviceResourceService.exist(applicationManifest.getAppId(), applicationManifest.getServiceId())) {
      throw new InvalidRequestException("Service doesn't exist");
    }

    ApplicationManifest savedApplicationManifest =
        wingsPersistence.saveAndGet(ApplicationManifest.class, applicationManifest);

    String appId = savedApplicationManifest.getAppId();
    String accountId = appService.getAccountIdByAppId(appId);
    Service service = serviceResourceService.get(appId, savedApplicationManifest.getServiceId());

    Type type = isCreate ? Type.CREATE : Type.UPDATE;
    yamlPushService.pushYamlChangeSet(
        accountId, service, savedApplicationManifest, type, savedApplicationManifest.isSyncFromGit());

    return savedApplicationManifest;
  }

  public ManifestFile upsertApplicationManifestFile(ManifestFile manifestFile, String serviceId, boolean isCreate) {
    if (!serviceResourceService.exist(manifestFile.getAppId(), serviceId)) {
      throw new InvalidRequestException("Service doesn't exist");
    }

    ApplicationManifest applicationManifest = getById(manifestFile.getAppId(), manifestFile.getApplicationManifestId());
    if (applicationManifest == null) {
      throw new InvalidRequestException("Application Manifest doesn't exist for Service: " + serviceId);
    }

    manifestFile.setApplicationManifestId(applicationManifest.getUuid());
    ManifestFile savedManifestFile =
        duplicateCheck(()
                           -> wingsPersistence.saveAndGet(ManifestFile.class, manifestFile),
            APP_MANIFEST_FILE_NAME, manifestFile.getFileName());

    String appId = applicationManifest.getAppId();
    String accountId = appService.getAccountIdByAppId(appId);
    Service service = serviceResourceService.get(appId, applicationManifest.getServiceId());

    Type type = isCreate ? Type.CREATE : Type.UPDATE;
    yamlPushService.pushYamlChangeSet(accountId, service, savedManifestFile, type, savedManifestFile.isSyncFromGit());

    return savedManifestFile;
  }

  @Override
  public void pruneByService(String appId, String serviceId) {
    ApplicationManifest applicationManifest = wingsPersistence.createQuery(ApplicationManifest.class)
                                                  .filter(ApplicationManifest.APP_ID_KEY, appId)
                                                  .filter(ApplicationManifest.SERVICE_ID_KEY, serviceId)
                                                  .get();

    if (applicationManifest != null) {
      deleteManifestFiles(appId, applicationManifest.getUuid());

      Query<ApplicationManifest> query = wingsPersistence.createQuery(ApplicationManifest.class)
                                             .filter(ApplicationManifest.APP_ID_KEY, appId)
                                             .filter(ApplicationManifest.ID_KEY, applicationManifest.getUuid());
      wingsPersistence.delete(query);
    }
  }

  @Override
  public void deleteManifestFiles(String appId, String applicationManifestId) {
    Query<ManifestFile> query = wingsPersistence.createQuery(ManifestFile.class)
                                    .filter(ManifestFile.APP_MANIFEST_ID_KEY, applicationManifestId)
                                    .filter(ManifestFile.APP_ID_KEY, appId);
    wingsPersistence.delete(query);
  }

  @Override
  public void deleteManifestFileById(String appId, String manifestFileId) {
    Query<ManifestFile> query = wingsPersistence.createQuery(ManifestFile.class)
                                    .filter(ManifestFile.APP_ID_KEY, appId)
                                    .filter(ManifestFile.ID_KEY, manifestFileId);
    wingsPersistence.delete(query);
  }

  private void validateApplicationManifest(ApplicationManifest applicationManifest) {
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

  public DirectoryNode getManifestFilesFromGit(String appId, String appManifestId) {
    Application app = appService.get(appId);
    ApplicationManifest appManifest = getById(appId, appManifestId);

    GitFetchFilesTaskParams fetchFilesTaskParams = k8sStateHelper.createGitFetchFilesTaskParams(app, appManifest);

    DelegateTask delegateTask = aDelegateTask()
                                    .withAccountId(app.getAccountId())
                                    .withAppId(app.getUuid())
                                    .withAsync(false)
                                    .withTaskType(TaskType.GIT_FETCH_FILES_TASK)
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

    List<ManifestFile> manifestFiles =
        manifestFilesFromGitFetchFilesResult((GitFetchFilesResult) executionResponse.getGitCommandResult(),
            fetchFilesTaskParams.getGitFileConfig().getFilePath());

    Service service = serviceResourceService.get(appId, appManifest.getServiceId());
    return yamlDirectoryService.generateManifestFileFolderNode(
        app.getAccountId(), service, manifestFiles, new DirectoryPath(MANIFEST_FILE_FOLDER));
  }
}
