package software.wings.sm.states.k8s;

import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.delegatetasks.k8s.Utils.getValuesYamlGitFilePath;
import static software.wings.delegatetasks.k8s.Utils.normalizeFilePath;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.ExecutionStatus;
import io.harness.exception.InvalidRequestException;
import software.wings.api.PhaseElement;
import software.wings.api.ServiceElement;
import software.wings.api.k8s.K8sContextElement;
import software.wings.api.k8s.K8sSetupExecutionData;
import software.wings.beans.Activity;
import software.wings.beans.Activity.Type;
import software.wings.beans.Application;
import software.wings.beans.ContainerInfrastructureMapping;
import software.wings.beans.DelegateTask;
import software.wings.beans.Environment;
import software.wings.beans.GitConfig;
import software.wings.beans.GitFetchFilesTaskParams;
import software.wings.beans.GitFileConfig;
import software.wings.beans.TaskType;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.StoreType;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.CommandUnitDetails.CommandUnitType;
import software.wings.beans.yaml.GitCommandExecutionResponse;
import software.wings.beans.yaml.GitFetchFilesResult;
import software.wings.beans.yaml.GitFile;
import software.wings.common.Constants;
import software.wings.helpers.ext.k8s.request.K8sDelegateManifestConfig;
import software.wings.helpers.ext.k8s.request.K8sDelegateManifestConfig.K8sDelegateManifestConfigBuilder;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.WorkflowStandardParams;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Singleton
public class K8sStateHelper {
  @Inject private transient ApplicationManifestService applicationManifestService;
  @Inject private transient SettingsService settingsService;
  @Inject private transient SecretManager secretManager;
  @Inject private transient DelegateService delegateService;
  @Inject private transient AppService appService;
  @Inject private transient InfrastructureMappingService infrastructureMappingService;

  public Activity createK8sActivity(ExecutionContext executionContext, String commandName, String stateType,
      ActivityService activityService, List<CommandUnit> commandUnits) {
    Application app = ((ExecutionContextImpl) executionContext).getApp();
    Environment env = ((ExecutionContextImpl) executionContext).getEnv();

    Activity activity = Activity.builder()
                            .applicationName(app.getName())
                            .appId(app.getUuid())
                            .commandName(commandName)
                            .type(Type.Command)
                            .workflowType(executionContext.getWorkflowType())
                            .workflowExecutionName(executionContext.getWorkflowExecutionName())
                            .stateExecutionInstanceId(executionContext.getStateExecutionInstanceId())
                            .stateExecutionInstanceName(executionContext.getStateExecutionInstanceName())
                            .commandType(stateType)
                            .workflowExecutionId(executionContext.getWorkflowExecutionId())
                            .workflowId(executionContext.getWorkflowId())
                            .commandUnits(commandUnits)
                            .status(ExecutionStatus.RUNNING)
                            .commandUnitType(CommandUnitType.KUBERNETES)
                            .environmentId(env.getUuid())
                            .environmentName(env.getName())
                            .environmentType(env.getEnvironmentType())
                            .build();

    return activityService.save(activity);
  }

  public K8sDelegateManifestConfig createDelegateManifestConfig(ApplicationManifest appManifest) {
    K8sDelegateManifestConfigBuilder manifestConfigBuilder =
        K8sDelegateManifestConfig.builder().manifestStoreTypes(appManifest.getStoreType());

    if (StoreType.Local.equals(appManifest.getStoreType())) {
      manifestConfigBuilder.manifestFiles(
          applicationManifestService.getManifestFilesByAppManifestId(appManifest.getAppId(), appManifest.getUuid()));
    } else {
      GitFileConfig gitFileConfig = appManifest.getGitFileConfig();
      GitConfig gitConfig = settingsService.fetchGitConfigFromConnectorId(gitFileConfig.getConnectorId());
      List<EncryptedDataDetail> encryptionDetails =
          secretManager.getEncryptionDetails(gitConfig, appManifest.getAppId(), null);

      gitFileConfig.setFilePath(normalizeFilePath(gitFileConfig.getFilePath()));
      manifestConfigBuilder.gitFileConfig(gitFileConfig);
      manifestConfigBuilder.gitConfig(gitConfig);
      manifestConfigBuilder.encryptedDataDetails(encryptionDetails);
    }

    return manifestConfigBuilder.build();
  }

  public ExecutionResponse executeGitTask(
      ExecutionContext context, ApplicationManifest applicationManifest, String activityId, String commandName) {
    Application app = appService.get(context.getAppId());

    GitFileConfig gitFileConfig = applicationManifest.getGitFileConfig();
    GitConfig gitConfig = settingsService.fetchGitConfigFromConnectorId(gitFileConfig.getConnectorId());

    gitFileConfig.setFilePath(getValuesYamlGitFilePath(gitFileConfig.getFilePath()));
    GitFetchFilesTaskParams fetchFilesTaskParams =
        GitFetchFilesTaskParams.builder()
            .accountId(app.getAccountId())
            .appId(app.getUuid())
            .activityId(activityId)
            .isFinalState(false)
            .gitConfig(gitConfig)
            .gitFileConfig(gitFileConfig)
            .encryptedDataDetails(secretManager.getEncryptionDetails(gitConfig, app.getUuid(), null))
            .build();

    DelegateTask delegateTask = aDelegateTask()
                                    .withAccountId(app.getAccountId())
                                    .withAppId(app.getUuid())
                                    .withTaskType(TaskType.GIT_FETCH_FILES_TASK)
                                    .withWaitId(activityId)
                                    .withAsync(true)
                                    .withParameters(new Object[] {fetchFilesTaskParams})
                                    // ToDo anshul decide on the timeout values
                                    .withTimeout(TimeUnit.MINUTES.toMillis(60))
                                    .build();

    String delegateTaskId = delegateService.queueTask(delegateTask);

    return ExecutionResponse.Builder.anExecutionResponse()
        .withAsync(true)
        .withCorrelationIds(Arrays.asList(activityId))
        .withStateExecutionData(K8sSetupExecutionData.builder().activityId(activityId).commandName(commandName).build())
        .withDelegateTaskId(delegateTaskId)
        .addContextElement(K8sContextElement.builder().currentTaskType(TaskType.GIT_COMMAND).build())
        .build();
  }

  public String getFileFromGitResponse(GitCommandExecutionResponse executionResponse) {
    GitFetchFilesResult gitFetchFilesResult = (GitFetchFilesResult) executionResponse.getGitCommandResult();
    List<GitFile> gitFiles = gitFetchFilesResult.getFiles();

    String valueYamlFromGit = null;
    if (!gitFiles.isEmpty()) {
      valueYamlFromGit = gitFiles.get(0).getFileContent();
    }

    return valueYamlFromGit;
  }

  public ApplicationManifest getApplicationManifest(ExecutionContext context) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    Application app = appService.get(context.getAppId());
    ServiceElement serviceElement = phaseElement.getServiceElement();

    ApplicationManifest applicationManifest = applicationManifestService.get(app.getUuid(), serviceElement.getUuid());
    if (applicationManifest == null) {
      throw new InvalidRequestException("Manifests not found for service.");
    }
    return applicationManifest;
  }

  public ContainerInfrastructureMapping getContainerInfrastructureMapping(ExecutionContext context) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    return (ContainerInfrastructureMapping) infrastructureMappingService.get(
        context.getAppId(), phaseElement.getInfraMappingId());
  }

  public Environment getEnvironment(ExecutionContext context) {
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    return workflowStandardParams.getEnv();
  }
}
