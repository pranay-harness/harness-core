package io.harness.cdng.k8s;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.beans.DelegateTask;
import io.harness.cdng.infra.yaml.Infrastructure;
import io.harness.cdng.infra.yaml.K8SDirectInfrastructure;
import io.harness.cdng.manifest.ManifestStoreType;
import io.harness.cdng.manifest.ManifestType;
import io.harness.cdng.manifest.yaml.FetchType;
import io.harness.cdng.manifest.yaml.GitStore;
import io.harness.cdng.manifest.yaml.ManifestAttributes;
import io.harness.cdng.manifest.yaml.StoreConfig;
import io.harness.cdng.manifest.yaml.kinds.K8sManifest;
import io.harness.cdng.manifest.yaml.kinds.ValuesManifest;
import io.harness.cdng.pipeline.K8sRollingStepInfo;
import io.harness.cdng.service.beans.ServiceOutcome;
import io.harness.cdng.tasks.manifestFetch.beans.GitFetchFilesConfig;
import io.harness.cdng.tasks.manifestFetch.beans.GitFetchRequest;
import io.harness.data.structure.UUIDGenerator;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.command.CommandExecutionResult;
import io.harness.engine.expressions.EngineExpressionService;
import io.harness.engine.outcomes.OutcomeService;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.execution.status.Status;
import io.harness.facilitator.PassThroughData;
import io.harness.facilitator.modes.chain.task.TaskChainExecutable;
import io.harness.facilitator.modes.chain.task.TaskChainResponse;
import io.harness.references.OutcomeRefObject;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.state.Step;
import io.harness.state.StepType;
import io.harness.state.io.FailureInfo;
import io.harness.state.io.K8sRollingStepParameters;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepParameters;
import io.harness.state.io.StepResponse;
import io.harness.validation.Validator;
import org.hibernate.validator.constraints.NotEmpty;
import org.jetbrains.annotations.NotNull;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.GitConfig;
import software.wings.beans.GitFileConfig;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.appmanifest.StoreType;
import software.wings.beans.yaml.GitCommandExecutionResponse;
import software.wings.beans.yaml.GitFetchFilesFromMultipleRepoResult;
import software.wings.beans.yaml.GitFetchFilesResult;
import software.wings.beans.yaml.GitFile;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig.K8sClusterConfigBuilder;
import software.wings.helpers.ext.k8s.request.K8sDelegateManifestConfig;
import software.wings.helpers.ext.k8s.request.K8sDelegateManifestConfig.K8sDelegateManifestConfigBuilder;
import software.wings.helpers.ext.k8s.request.K8sRollingDeployTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.states.k8s.K8sRollingDeploy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class K8sRollingStep implements Step, TaskChainExecutable {
  public static final StepType STEP_TYPE = StepType.builder().type("K8S_ROLLING").build();

  @Inject private SecretManager secretManager;
  @Inject private SettingsService settingsService;
  @Inject private OutcomeService outcomeService;
  @Inject private EngineExpressionService engineExpressionService;

  @Override
  public TaskChainResponse startChainLink(
      Ambiance ambiance, StepParameters stepParameters, StepInputPackage inputPackage) {
    K8sRollingStepParameters k8sRollingStepParameters = ((K8sRollingStepInfo) stepParameters).getK8sRolling();

    ServiceOutcome serviceOutcome =
        (ServiceOutcome) outcomeService.resolve(ambiance, OutcomeRefObject.builder().name("service").build());
    if (serviceOutcome == null) {
      throw new InvalidRequestException("Services step need to run before k8s rolling step", WingsException.ADMIN);
    }

    Infrastructure infrastructure =
        (Infrastructure) outcomeService.resolve(ambiance, OutcomeRefObject.builder().name("infrastructure").build());
    if (infrastructure == null) {
      throw new InvalidRequestException(
          "Infrastructure step need to run before k8s rolling step", WingsException.ADMIN);
    }

    List<ManifestAttributes> serviceManifests = serviceOutcome.getManifests();
    Validator.notEmptyCheck("Service Level Manifests can't be empty", serviceManifests);
    List<ManifestAttributes> overrideManifests =
        serviceOutcome.getOverrides() == null ? Collections.emptyList() : serviceOutcome.getOverrides().getManifests();

    K8sManifest k8sManifest = getK8sManifest(serviceManifests);
    List<ValuesManifest> aggregatedValuesManifests = getAggregatedValuesManifests(serviceManifests, overrideManifests);

    if (isEmpty(aggregatedValuesManifests)) {
      return executeK8sTask(k8sManifest, ambiance, k8sRollingStepParameters, Collections.emptyList(), infrastructure);
    }

    if (!isAnyRemoteStore(aggregatedValuesManifests)) {
      List<String> valuesFileContentsForLocalStore = getValuesFileContentsForLocalStore(aggregatedValuesManifests);
      return executeK8sTask(
          k8sManifest, ambiance, k8sRollingStepParameters, valuesFileContentsForLocalStore, infrastructure);
    }

    return executeValuesFetchTask(
        ambiance, k8sRollingStepParameters, infrastructure, k8sManifest, aggregatedValuesManifests);
  }

  private TaskChainResponse executeValuesFetchTask(Ambiance ambiance, K8sRollingStepParameters k8sRollingStepParameters,
      Infrastructure infrastructure, K8sManifest k8sManifest, List<ValuesManifest> aggregatedValuesManifests) {
    List<GitFetchFilesConfig> gitFetchFilesConfigs = new ArrayList<>();

    for (ValuesManifest valuesManifest : aggregatedValuesManifests) {
      if (ManifestStoreType.GIT.equals(valuesManifest.getStoreConfig().getKind())) {
        GitStore gitStore = (GitStore) valuesManifest.getStoreConfig();
        String connectorId = gitStore.getConnectorId();
        GitConfig gitConfig = (GitConfig) getSettingAttribute(connectorId).getValue();

        List<EncryptedDataDetail> encryptionDetails = getEncryptedDataDetails(gitConfig);
        gitFetchFilesConfigs.add(GitFetchFilesConfig.builder()
                                     .encryptedDataDetails(encryptionDetails)
                                     .gitConfig(gitConfig)
                                     .gitStore(gitStore)
                                     .identifier(valuesManifest.getIdentifier())
                                     .paths(gitStore.getPaths())
                                     .succeedIfFileNotFound(false)
                                     .build());
      }
    }

    String accountId = ambiance.getSetupAbstractions().get("accountId");
    GitFetchRequest gitFetchRequest =
        GitFetchRequest.builder().gitFetchFilesConfigs(gitFetchFilesConfigs).accountId(accountId).build();

    String waitId = generateUuid();
    TaskData taskData = TaskData.builder()
                            .async(true)
                            .timeout(k8sRollingStepParameters.getTimeout())
                            .taskType(TaskType.GIT_FETCH_NEXT_GEN_TASK.name())
                            .parameters(new Object[] {gitFetchRequest})
                            .build();

    DelegateTask delegateTask = DelegateTask.builder().accountId(accountId).waitId(waitId).data(taskData).build();

    K8sRollingStepPassThroughData k8sRollingStepPassThroughData = K8sRollingStepPassThroughData.builder()
                                                                      .k8sManifest(k8sManifest)
                                                                      .valuesManifests(aggregatedValuesManifests)
                                                                      .infrastructure(infrastructure)
                                                                      .build();
    return TaskChainResponse.builder()
        .chainEnd(false)
        .task(delegateTask)
        .passThroughData(k8sRollingStepPassThroughData)
        .build();
  }

  private List<EncryptedDataDetail> getEncryptedDataDetails(EncryptableSetting encryptableSetting) {
    // TODO: move to new secret manager apis without app and workflowIds
    return secretManager.getEncryptionDetails(encryptableSetting, "", null);
  }

  private List<String> getValuesFileContentsForLocalStore(List<ValuesManifest> aggregatedValuesManifests) {
    // TODO: implement when local store is available
    return Collections.emptyList();
  }

  private SettingAttribute getSettingAttribute(String connectorId) {
    // TODO: change when Connectors NG comes up
    return settingsService.get(connectorId);
  }

  private TaskChainResponse executeK8sTask(K8sManifest k8sManifest, Ambiance ambiance,
      K8sRollingStepParameters stepParameters, List<String> valuesFileContents, Infrastructure infrastructure) {
    List<String> renderedValuesList = renderValues(ambiance, valuesFileContents);
    StoreConfig storeConfig = k8sManifest.getStoreConfig();

    K8sDelegateManifestConfig k8sDelegateManifestConfig = getK8sDelegateManifestConfig(storeConfig);
    K8sClusterConfig k8sClusterConfig = getK8sClusterConfig(infrastructure);
    String releaseName = getReleaseName(infrastructure);

    K8sRollingDeployTaskParameters k8sRollingDeployTaskParameters =
        K8sRollingDeployTaskParameters.builder()
            .skipDryRun(stepParameters.isSkipDryRun())
            .isInCanaryWorkflow(false)
            .k8sDelegateManifestConfig(k8sDelegateManifestConfig)
            .releaseName(releaseName)
            .activityId(UUIDGenerator.generateUuid())
            .commandName(K8sRollingDeploy.K8S_ROLLING_DEPLOY_COMMAND_NAME)
            .k8sTaskType(K8sTaskParameters.K8sTaskType.DEPLOYMENT_ROLLING)
            .localOverrideFeatureFlag(false)
            .timeoutIntervalInMin(stepParameters.getTimeout())
            .valuesYamlList(renderedValuesList)
            .accountId(ambiance.getSetupAbstractions().get("accountId"))
            .k8sClusterConfig(k8sClusterConfig)
            .activityId(UUIDGenerator.generateUuid())
            .build();

    TaskData taskData = TaskData.builder()
                            .parameters(new Object[] {k8sRollingDeployTaskParameters})
                            .taskType(TaskType.K8S_COMMAND_TASK.name())
                            .timeout(stepParameters.getTimeout())
                            .async(true)
                            .build();

    DelegateTask delegateTask = DelegateTask.builder()
                                    .data(taskData)
                                    .accountId(ambiance.getSetupAbstractions().get("accountId"))
                                    .waitId(UUIDGenerator.generateUuid())
                                    .build();

    return TaskChainResponse.builder().task(delegateTask).chainEnd(true).build();
  }

  private List<String> renderValues(Ambiance ambiance, List<String> valuesFileContents) {
    if (isEmpty(valuesFileContents)) {
      return Collections.emptyList();
    }

    return valuesFileContents.stream()
        .map(valuesFileContent -> engineExpressionService.renderExpression(ambiance, valuesFileContent))
        .collect(Collectors.toList());
  }

  private String getReleaseName(Infrastructure infrastructure) {
    switch (infrastructure.getKind()) {
      case K8S_DIRECT:
        K8SDirectInfrastructure k8SDirectInfrastructure = (K8SDirectInfrastructure) infrastructure;
        return k8SDirectInfrastructure.getReleaseName();
      default:
        throw new UnsupportedOperationException(
            String.format("Unknown infrastructure type: [%s]", infrastructure.getKind()));
    }
  }

  private K8sDelegateManifestConfig getK8sDelegateManifestConfig(StoreConfig storeConfig) {
    K8sDelegateManifestConfigBuilder k8sDelegateManifestConfigBuilder = K8sDelegateManifestConfig.builder();

    if (storeConfig.getKind().equals(ManifestStoreType.GIT)) {
      StoreType storeType = StoreType.Remote;
      GitStore gitStore = (GitStore) storeConfig;
      SettingAttribute gitConfigSettingAttribute = getSettingAttribute(gitStore.getConnectorId());
      List<EncryptedDataDetail> encryptionDetails =
          getEncryptedDataDetails((GitConfig) gitConfigSettingAttribute.getValue());

      GitFileConfig gitFileConfig =
          GitFileConfig.builder()
              .connectorId(gitStore.getConnectorId())
              .useBranch(gitStore.getFetchType() == FetchType.BRANCH)
              .branch(gitStore.getFetchType() == FetchType.BRANCH ? gitStore.getFetchValue() : null)
              .filePathList(gitStore.getPaths())
              .filePath(isNotEmpty(gitStore.getPaths()) ? gitStore.getPaths().get(0) : null)
              .commitId(gitStore.getFetchType() == FetchType.BRANCH ? null : gitStore.getFetchValue())
              .build();

      k8sDelegateManifestConfigBuilder.gitConfig((GitConfig) gitConfigSettingAttribute.getValue())
          .manifestStoreTypes(storeType)
          .encryptedDataDetails(encryptionDetails)
          .gitFileConfig(gitFileConfig);
    }
    // TODO: local store preparation later
    return k8sDelegateManifestConfigBuilder.build();
  }

  private K8sClusterConfig getK8sClusterConfig(Infrastructure infrastructure) {
    K8sClusterConfigBuilder k8sClusterConfigBuilder = K8sClusterConfig.builder();

    switch (infrastructure.getKind()) {
      case K8S_DIRECT:
        K8SDirectInfrastructure k8SDirectInfrastructure = (K8SDirectInfrastructure) infrastructure;
        SettingAttribute cloudProvider = getSettingAttribute(k8SDirectInfrastructure.getConnectorId());
        List<EncryptedDataDetail> encryptionDetails =
            getEncryptedDataDetails((KubernetesClusterConfig) cloudProvider.getValue());
        k8sClusterConfigBuilder.cloudProvider(cloudProvider.getValue())
            .namespace(k8SDirectInfrastructure.getNamespace())
            .cloudProviderEncryptionDetails(encryptionDetails)
            .cloudProviderName(cloudProvider.getName());
        return k8sClusterConfigBuilder.build();
      default:
        throw new UnsupportedOperationException(
            String.format("Unknown infrastructure type: [%s]", infrastructure.getKind()));
    }
  }

  private boolean isAnyRemoteStore(@NotEmpty List<ValuesManifest> aggregatedValuesManifests) {
    return aggregatedValuesManifests.stream().anyMatch(
        valuesManifest -> ManifestStoreType.GIT.equals(valuesManifest.getStoreConfig().getKind()));
  }

  List<ValuesManifest> getAggregatedValuesManifests(
      @NotEmpty List<ManifestAttributes> serviceManifests, List<ManifestAttributes> overrideManifests) {
    List<ValuesManifest> aggregateValuesManifests = new ArrayList<>();

    addValuesEntryForK8ManifestIfNeeded(serviceManifests, aggregateValuesManifests);

    List<ValuesManifest> serviceValuesManifests =
        serviceManifests.stream()
            .filter(manifestAttribute -> ManifestType.VALUES.equals(manifestAttribute.getKind()))
            .map(manifestAttribute -> (ValuesManifest) manifestAttribute)
            .collect(Collectors.toList());

    if (isNotEmpty(serviceValuesManifests)) {
      aggregateValuesManifests.addAll(serviceValuesManifests);
    }

    if (isNotEmpty(overrideManifests)) {
      List<ValuesManifest> overridesValuesManifests =
          overrideManifests.stream()
              .filter(manifestAttribute -> ManifestType.VALUES.equals(manifestAttribute.getKind()))
              .map(manifestAttribute -> (ValuesManifest) manifestAttribute)
              .collect(Collectors.toList());

      if (isNotEmpty(overridesValuesManifests)) {
        aggregateValuesManifests.addAll(overridesValuesManifests);
      }
    }

    return aggregateValuesManifests;
  }

  private void addValuesEntryForK8ManifestIfNeeded(
      List<ManifestAttributes> serviceManifests, List<ValuesManifest> aggregateValuesManifests) {
    K8sManifest k8sManifest =
        (K8sManifest) serviceManifests.stream()
            .filter(manifestAttribute -> ManifestType.K8Manifest.equals(manifestAttribute.getKind()))
            .findFirst()
            .orElse(null);

    if (k8sManifest != null && isNotEmpty(k8sManifest.getValuesFilePaths())
        && ManifestStoreType.GIT.equals(k8sManifest.getStoreConfig().getKind())) {
      GitStore gitStore = (GitStore) k8sManifest.getStoreConfig();
      ValuesManifest valuesManifest = ValuesManifest.builder()
                                          .identifier(k8sManifest.getIdentifier())
                                          .storeConfig(gitStore.cloneInternal())
                                          .build();

      ((GitStore) valuesManifest.getStoreConfig()).setPaths(k8sManifest.getValuesFilePaths());

      aggregateValuesManifests.add(valuesManifest);
    }
  }

  K8sManifest getK8sManifest(@NotEmpty List<ManifestAttributes> serviceManifests) {
    List<ManifestAttributes> k8sManifests =
        serviceManifests.stream()
            .filter(manifestAttribute -> ManifestType.K8Manifest.equals(manifestAttribute.getKind()))
            .collect(Collectors.toList());

    if (isEmpty(k8sManifests)) {
      throw new InvalidRequestException("K8s Manifests are mandatory for k8s Rolling step", WingsException.USER);
    }

    if (k8sManifests.size() > 1) {
      throw new InvalidRequestException("There can be only a single K8s manifest", WingsException.USER);
    }
    return (K8sManifest) k8sManifests.get(0);
  }

  @Override
  public TaskChainResponse executeNextLink(Ambiance ambiance, StepParameters stepParameters,
      StepInputPackage inputPackage, PassThroughData passThroughData, Map<String, ResponseData> responseDataMap) {
    GitCommandExecutionResponse gitTaskResponse =
        (GitCommandExecutionResponse) responseDataMap.values().iterator().next();

    if (gitTaskResponse.getGitCommandStatus() != GitCommandExecutionResponse.GitCommandStatus.SUCCESS) {
      throw new InvalidRequestException(gitTaskResponse.getErrorMessage());
    }
    Map<String, GitFetchFilesResult> gitFetchFilesResultMap =
        ((GitFetchFilesFromMultipleRepoResult) gitTaskResponse.getGitCommandResult()).getFilesFromMultipleRepo();

    K8sRollingStepPassThroughData k8sRollingPassThroughData = (K8sRollingStepPassThroughData) passThroughData;

    K8sManifest k8sManifest = k8sRollingPassThroughData.getK8sManifest();
    List<ValuesManifest> valuesManifests = k8sRollingPassThroughData.getValuesManifests();

    List<String> valuesFileContents = getFileContents(gitFetchFilesResultMap, valuesManifests);

    K8sRollingStepParameters k8sRollingStepParameters = ((K8sRollingStepInfo) stepParameters).getK8sRolling();
    return executeK8sTask(k8sManifest, ambiance, k8sRollingStepParameters, valuesFileContents,
        k8sRollingPassThroughData.getInfrastructure());
  }

  @NotNull
  List<String> getFileContents(
      Map<String, GitFetchFilesResult> gitFetchFilesResultMap, List<ValuesManifest> valuesManifests) {
    List<String> valuesFileContents = new ArrayList<>();

    for (ValuesManifest valuesManifest : valuesManifests) {
      if (ManifestStoreType.GIT.equals(valuesManifest.getStoreConfig().getKind())) {
        GitFetchFilesResult gitFetchFilesResult = gitFetchFilesResultMap.get(valuesManifest.getIdentifier());
        valuesFileContents.addAll(
            gitFetchFilesResult.getFiles().stream().map(GitFile::getFileContent).collect(Collectors.toList()));
      }
      // TODO: for local store, add files directly
    }
    return valuesFileContents;
  }

  @Override
  public StepResponse finalizeExecution(Ambiance ambiance, StepParameters stepParameters,
      PassThroughData passThroughData, Map<String, ResponseData> responseDataMap) {
    K8sTaskExecutionResponse k8sTaskExecutionResponse =
        (K8sTaskExecutionResponse) responseDataMap.values().iterator().next();

    if (k8sTaskExecutionResponse.getCommandExecutionStatus() == CommandExecutionResult.CommandExecutionStatus.SUCCESS) {
      return StepResponse.builder().status(Status.SUCCEEDED).build();
    } else {
      return StepResponse.builder()
          .status(Status.FAILED)
          .failureInfo(FailureInfo.builder().errorMessage(k8sTaskExecutionResponse.getErrorMessage()).build())
          .build();
    }
  }
}
