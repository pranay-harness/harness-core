package software.wings.sm.states.provision;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;

import static software.wings.beans.command.AzureARMCommandUnit.ExcuteDeployment;
import static software.wings.beans.command.AzureARMCommandUnit.FetchFiles;
import static software.wings.beans.command.CommandUnitDetails.CommandUnitType.AZURE_ARM_DEPLOYMENT;

import io.harness.beans.ExecutionStatus;
import io.harness.beans.TriggeredBy;
import io.harness.context.ContextElementType;
import io.harness.exception.InvalidRequestException;
import io.harness.git.model.GitFile;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.api.ARMStateExecutionData;
import software.wings.beans.ARMInfrastructureProvisioner;
import software.wings.beans.ARMSourceType;
import software.wings.beans.Activity;
import software.wings.beans.Activity.Type;
import software.wings.beans.GitConfig;
import software.wings.beans.GitFetchFilesConfig;
import software.wings.beans.GitFileConfig;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.command.AzureARMCommandUnit;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.yaml.GitFetchFilesFromMultipleRepoResult;
import software.wings.beans.yaml.GitFetchFilesResult;
import software.wings.service.impl.GitConfigHelperService;
import software.wings.service.impl.GitFileConfigHelperService;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.InfrastructureProvisionerService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionContext;
import software.wings.sm.WorkflowStandardParams;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class ARMStateHelper {
  public static final String AZURE_ARM_COMMAND_UNIT_TYPE = "ARM Deployment";

  @Inject private SecretManager secretManager;
  @Inject private ActivityService activityService;
  @Inject private SettingsService settingsService;
  @Inject private GitConfigHelperService gitConfigHelperService;
  @Inject private GitFileConfigHelperService gitFileConfigHelperService;
  @Inject private InfrastructureProvisionerService infrastructureProvisionerService;

  ARMInfrastructureProvisioner getProvisioner(String appId, String provisionerId) {
    InfrastructureProvisioner infrastructureProvisioner = infrastructureProvisionerService.get(appId, provisionerId);
    if (!(infrastructureProvisioner instanceof ARMInfrastructureProvisioner)) {
      throw new InvalidRequestException("Provisioner is not an ARM provisioner");
    }
    return (ARMInfrastructureProvisioner) infrastructureProvisioner;
  }

  boolean executeGitTask(ARMInfrastructureProvisioner provisioner, GitFileConfig variablesGitFileConfig) {
    return ARMSourceType.GIT == provisioner.getSourceType() || variablesGitFileConfig != null;
  }

  private List<CommandUnit> getCommandUnits(boolean executeGitTask) {
    List<CommandUnit> commandUnits = new ArrayList<>();
    if (executeGitTask) {
      commandUnits.add(new AzureARMCommandUnit(FetchFiles));
    }
    commandUnits.add(new AzureARMCommandUnit(ExcuteDeployment));
    return commandUnits;
  }

  Activity createActivity(ExecutionContext context, boolean executeGitTask, String commandType) {
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    notNullCheck("WorkflowStandardParams are NULL", workflowStandardParams, USER);
    notNullCheck("CurrentUser is NULL", workflowStandardParams.getCurrentUser(), USER);

    Activity activity = Activity.builder()
                            .applicationName(context.fetchRequiredApp().getName())
                            .appId(context.getAppId())
                            .commandName("Execute ARM Deployment")
                            .type(Type.Command)
                            .workflowType(context.getWorkflowType())
                            .workflowExecutionName(context.getWorkflowExecutionName())
                            .stateExecutionInstanceId(context.getStateExecutionInstanceId())
                            .stateExecutionInstanceName(context.getStateExecutionInstanceName())
                            .commandType(commandType)
                            .workflowExecutionId(context.getWorkflowExecutionId())
                            .workflowId(context.getWorkflowId())
                            .commandUnits(getCommandUnits(executeGitTask))
                            .status(ExecutionStatus.RUNNING)
                            .commandUnitType(AZURE_ARM_DEPLOYMENT)
                            .environmentId(context.fetchRequiredEnvironment().getUuid())
                            .environmentName(context.fetchRequiredEnvironment().getName())
                            .environmentType(context.fetchRequiredEnvironment().getEnvironmentType())
                            .triggeredBy(TriggeredBy.builder()
                                             .email(workflowStandardParams.getCurrentUser().getEmail())
                                             .name(workflowStandardParams.getCurrentUser().getName())
                                             .build())
                            .build();
    return activityService.save(activity);
  }

  GitFetchFilesConfig createGitFetchFilesConfig(GitFileConfig gitFileConfigRaw, ExecutionContext context) {
    GitFileConfig gitFileConfig = gitFileConfigHelperService.renderGitFileConfig(context, gitFileConfigRaw);
    GitConfig gitConfig = settingsService.fetchGitConfigFromConnectorId(gitFileConfig.getConnectorId());
    notNullCheck("Git config not found", gitConfig);
    gitConfigHelperService.convertToRepoGitConfig(gitConfig, gitFileConfig.getRepoName());
    List<EncryptedDataDetail> encryptionDetails =
        secretManager.getEncryptionDetails(gitConfig, context.getAppId(), context.getWorkflowExecutionId());
    return GitFetchFilesConfig.builder()
        .gitConfig(gitConfig)
        .gitFileConfig(gitFileConfig)
        .encryptedDataDetails(encryptionDetails)
        .build();
  }

  String extractJsonFromGitResponse(ARMStateExecutionData stateExecutionData, String key) {
    notNullCheck("State Execution Data is null when extracting Git Response", stateExecutionData);
    GitFetchFilesFromMultipleRepoResult fetchFilesResult = stateExecutionData.getFetchFilesResult();
    notNullCheck("Git Fetch from multiple REPOS is null when extracting Git Response", fetchFilesResult);
    Map<String, GitFetchFilesResult> filesFromMultipleRepo = fetchFilesResult.getFilesFromMultipleRepo();
    if (isEmpty(filesFromMultipleRepo) || (!filesFromMultipleRepo.containsKey(key))) {
      throw new InvalidRequestException(String.format("Files for [%s] not found", key));
    }
    List<GitFile> files = filesFromMultipleRepo.get(key).getFiles();
    if (isEmpty(files)) {
      throw new InvalidRequestException(String.format("Files for [%s] not found", key));
    }
    return files.get(0).getFileContent();
  }
}
