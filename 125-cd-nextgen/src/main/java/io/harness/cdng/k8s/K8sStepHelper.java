package io.harness.cdng.k8s;

import static io.harness.cdng.infra.yaml.InfrastructureKind.KUBERNETES_DIRECT;
import static io.harness.connector.ConnectorModule.DEFAULT_CONNECTOR_SERVICE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.ngpipeline.common.ParameterFieldHelper.getParameterFieldValue;
import static io.harness.steps.StepUtils.prepareTaskRequest;

import static java.lang.String.format;

import io.harness.beans.IdentifierRef;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.infra.beans.K8sDirectInfrastructureOutcome;
import io.harness.cdng.manifest.ManifestStoreType;
import io.harness.cdng.manifest.ManifestType;
import io.harness.cdng.manifest.yaml.GitStore;
import io.harness.cdng.manifest.yaml.K8sManifestOutcome;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.cdng.manifest.yaml.StoreConfig;
import io.harness.cdng.manifest.yaml.ValuesManifestOutcome;
import io.harness.cdng.service.beans.ServiceOutcome;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.common.NGTaskType;
import io.harness.common.NGTimeConversionHelper;
import io.harness.connector.apis.dto.ConnectorInfoDTO;
import io.harness.connector.apis.dto.ConnectorResponseDTO;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthCredentialDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType;
import io.harness.delegate.beans.connector.k8Connector.KubernetesUserNamePasswordDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitHTTPAuthenticationDTO;
import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;
import io.harness.delegate.task.git.GitFetchFilesConfig;
import io.harness.delegate.task.git.GitFetchRequest;
import io.harness.delegate.task.git.GitFetchResponse;
import io.harness.delegate.task.git.TaskStatus;
import io.harness.delegate.task.k8s.DirectK8sInfraDelegateConfig;
import io.harness.delegate.task.k8s.K8sDeployRequest;
import io.harness.delegate.task.k8s.K8sInfraDelegateConfig;
import io.harness.delegate.task.k8s.K8sManifestDelegateConfig;
import io.harness.delegate.task.k8s.ManifestDelegateConfig;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.executions.steps.LoggingMetadata;
import io.harness.git.model.FetchFilesResult;
import io.harness.git.model.GitFile;
import io.harness.k8s.K8sCommandUnitConstants;
import io.harness.k8s.model.KubernetesClusterAuthType;
import io.harness.ng.core.NGAccess;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.tasks.TaskCategory;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.expression.EngineExpressionService;
import io.harness.pms.sdk.core.resolver.RefObjectUtil;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.pms.sdk.core.steps.executables.TaskChainResponse;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.KryoSerializer;
import io.harness.shell.AuthenticationScheme;
import io.harness.steps.StepUtils;
import io.harness.tasks.ResponseData;
import io.harness.utils.IdentifierRefHelper;
import io.harness.validation.Validator;

import software.wings.annotation.EncryptableSetting;
import software.wings.beans.GitConfig;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.LogHelper;
import software.wings.beans.SettingAttribute;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig.K8sClusterConfigBuilder;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;

@Singleton
public class K8sStepHelper {
  @Named(DEFAULT_CONNECTOR_SERVICE) @Inject private ConnectorService connectorService;
  @Inject private SecretManagerClientService secretManagerClientService;
  @Inject private EngineExpressionService engineExpressionService;
  @Inject private KryoSerializer kryoSerializer;
  @Inject private OutcomeService outcomeService;

  String getReleaseName(InfrastructureOutcome infrastructure) {
    switch (infrastructure.getKind()) {
      case KUBERNETES_DIRECT:
        K8sDirectInfrastructureOutcome k8SDirectInfrastructure = (K8sDirectInfrastructureOutcome) infrastructure;
        return k8SDirectInfrastructure.getReleaseName();
      default:
        throw new UnsupportedOperationException(format("Unknown infrastructure type: [%s]", infrastructure.getKind()));
    }
  }

  public ConnectorInfoDTO getConnector(String connectorId, Ambiance ambiance) {
    NGAccess ngAccess = AmbianceHelper.getNgAccess(ambiance);
    IdentifierRef identifierRef = IdentifierRefHelper.getIdentifierRef(
        connectorId, ngAccess.getAccountIdentifier(), ngAccess.getOrgIdentifier(), ngAccess.getProjectIdentifier());
    Optional<ConnectorResponseDTO> connectorDTO = connectorService.get(identifierRef.getAccountIdentifier(),
        identifierRef.getOrgIdentifier(), identifierRef.getProjectIdentifier(), identifierRef.getIdentifier());
    if (!connectorDTO.isPresent()) {
      throw new InvalidRequestException(
          format("Connector not found for identifier : [%s]", connectorId), WingsException.USER);
    }
    return connectorDTO.get().getConnector();
  }

  List<EncryptedDataDetail> getEncryptedDataDetails(EncryptableSetting encryptableSetting) {
    return secretManagerClientService.getEncryptionDetails(encryptableSetting);
  }

  K8sClusterConfig getK8sClusterConfig(InfrastructureOutcome infrastructure, Ambiance ambiance) {
    K8sClusterConfigBuilder k8sClusterConfigBuilder = K8sClusterConfig.builder();

    switch (infrastructure.getKind()) {
      case KUBERNETES_DIRECT:
        K8sDirectInfrastructureOutcome k8SDirectInfrastructure = (K8sDirectInfrastructureOutcome) infrastructure;
        SettingAttribute cloudProvider = getSettingAttribute(k8SDirectInfrastructure.getConnectorRef(), ambiance);
        List<EncryptedDataDetail> encryptionDetails =
            getEncryptedDataDetails((KubernetesClusterConfig) cloudProvider.getValue());
        k8sClusterConfigBuilder.cloudProvider(cloudProvider.getValue())
            .namespace(k8SDirectInfrastructure.getNamespace())
            .cloudProviderEncryptionDetails(encryptionDetails)
            .cloudProviderName(cloudProvider.getName());
        return k8sClusterConfigBuilder.build();
      default:
        throw new UnsupportedOperationException(format("Unknown infrastructure type: [%s]", infrastructure.getKind()));
    }
  }

  private SettingAttribute getSettingAttribute(@NotNull ConnectorInfoDTO connectorDTO) {
    SettingAttribute.Builder builder = SettingAttribute.Builder.aSettingAttribute().withName(connectorDTO.getName());
    switch (connectorDTO.getConnectorType()) {
      case KUBERNETES_CLUSTER:
        KubernetesClusterConfigDTO connectorConfig = (KubernetesClusterConfigDTO) connectorDTO.getConnectorConfig();
        KubernetesClusterDetailsDTO config = (KubernetesClusterDetailsDTO) connectorConfig.getCredential().getConfig();
        KubernetesUserNamePasswordDTO auth = (KubernetesUserNamePasswordDTO) config.getAuth().getCredentials();
        // todo @Vaibhav/@Deepak: Now the k8 uses the new secret and this secret requires identifier and previous
        // required uuid, this has to be changed according to the framework
        KubernetesClusterConfig kubernetesClusterConfig =
            KubernetesClusterConfig.builder()
                .authType(KubernetesClusterAuthType.USER_PASSWORD)
                .masterUrl(config.getMasterUrl())
                .username(auth.getUsername() != null ? auth.getUsername().toCharArray() : null)
                .build();
        builder.withValue(kubernetesClusterConfig);
        break;
      case GIT:
        GitConfigDTO gitConfigDTO = (GitConfigDTO) connectorDTO.getConnectorConfig();
        GitHTTPAuthenticationDTO gitAuth = (GitHTTPAuthenticationDTO) gitConfigDTO.getGitAuth();
        GitConfig gitConfig =
            GitConfig.builder()
                .repoUrl(gitConfigDTO.getUrl())
                .username(gitAuth.getUsername())
                // todo @Vaibhav/@Deepak: Now the git uses the new secret and this secret requires identifier and
                // previous required uuid, this has to be changed according to the framework
                /* .encryptedPassword(SecretRefHelper.getSecretConfigString())*/
                .branch(gitConfigDTO.getBranchName())
                .authenticationScheme(AuthenticationScheme.HTTP_PASSWORD)
                .build();
        builder.withValue(gitConfig);
        break;
      default:
    }
    return builder.build();
  }

  SettingAttribute getSettingAttribute(String connectorId, Ambiance ambiance) {
    ConnectorInfoDTO connectorDTO = getConnector(connectorId, ambiance);
    return getSettingAttribute(connectorDTO);
  }

  public ManifestDelegateConfig getManifestDelegateConfig(StoreConfig storeConfig, Ambiance ambiance) {
    if (storeConfig.getKind().equals(ManifestStoreType.GIT)) {
      GitStore gitStore = (GitStore) storeConfig;
      ConnectorInfoDTO connectorDTO = getConnector(getParameterFieldValue(gitStore.getConnectorRef()), ambiance);
      GitConfigDTO gitConfigDTO = (GitConfigDTO) connectorDTO.getConnectorConfig();

      NGAccess basicNGAccessObject = AmbianceHelper.getNgAccess(ambiance);
      List<EncryptedDataDetail> encryptedDataDetailList =
          secretManagerClientService.getEncryptionDetails(basicNGAccessObject, gitConfigDTO.getGitAuth());

      return K8sManifestDelegateConfig.builder()
          .storeDelegateConfig(getGitStoreDelegateConfig(gitStore, connectorDTO, encryptedDataDetailList))
          .build();
    } else {
      throw new UnsupportedOperationException(format("Unsupported Store Config type: [%s]", storeConfig.getKind()));
    }
  }

  public GitStoreDelegateConfig getGitStoreDelegateConfig(@Nonnull GitStore gitStore,
      @Nonnull ConnectorInfoDTO connectorDTO, @Nonnull List<EncryptedDataDetail> encryptedDataDetailList) {
    return GitStoreDelegateConfig.builder()
        .gitConfigDTO((GitConfigDTO) connectorDTO.getConnectorConfig())
        .encryptedDataDetails(encryptedDataDetailList)
        .fetchType(gitStore.getGitFetchType())
        .branch(getParameterFieldValue(gitStore.getBranch()))
        .commitId(getParameterFieldValue(gitStore.getCommitId()))
        .paths(getParameterFieldValue(gitStore.getPaths()))
        .connectorName(connectorDTO.getName())
        .build();
  }

  private List<EncryptedDataDetail> getEncryptionDataDetails(
      @Nonnull ConnectorInfoDTO connectorDTO, @Nonnull NGAccess ngAccess) {
    switch (connectorDTO.getConnectorType()) {
      case KUBERNETES_CLUSTER:
        KubernetesClusterConfigDTO connectorConfig = (KubernetesClusterConfigDTO) connectorDTO.getConnectorConfig();
        if (connectorConfig.getCredential().getKubernetesCredentialType()
            == KubernetesCredentialType.MANUAL_CREDENTIALS) {
          KubernetesClusterDetailsDTO clusterDetailsDTO =
              (KubernetesClusterDetailsDTO) connectorConfig.getCredential().getConfig();

          KubernetesAuthCredentialDTO authCredentialDTO = clusterDetailsDTO.getAuth().getCredentials();
          return secretManagerClientService.getEncryptionDetails(ngAccess, authCredentialDTO);
        } else {
          return Collections.emptyList();
        }
      case APP_DYNAMICS:
      case SPLUNK:
      case GIT:
      default:
        throw new UnsupportedOperationException(
            format("Unsupported connector type : [%s]", connectorDTO.getConnectorType()));
    }
  }

  public K8sInfraDelegateConfig getK8sInfraDelegateConfig(InfrastructureOutcome infrastructure, Ambiance ambiance) {
    switch (infrastructure.getKind()) {
      case KUBERNETES_DIRECT:
        K8sDirectInfrastructureOutcome k8SDirectInfrastructure = (K8sDirectInfrastructureOutcome) infrastructure;
        ConnectorInfoDTO connectorDTO = getConnector(k8SDirectInfrastructure.getConnectorRef(), ambiance);

        return DirectK8sInfraDelegateConfig.builder()
            .namespace(k8SDirectInfrastructure.getNamespace())
            .kubernetesClusterConfigDTO((KubernetesClusterConfigDTO) connectorDTO.getConnectorConfig())
            .encryptionDataDetails(getEncryptionDataDetails(connectorDTO, AmbianceHelper.getNgAccess(ambiance)))
            .build();

      default:
        throw new UnsupportedOperationException(
            format("Unsupported Infrastructure type: [%s]", infrastructure.getKind()));
    }
  }

  public List<EncryptedDataDetail> getEncryptedDataDetails(
      @Nonnull GitConfigDTO gitConfigDTO, @Nonnull Ambiance ambiance) {
    return secretManagerClientService.getEncryptionDetails(
        AmbianceHelper.getNgAccess(ambiance), gitConfigDTO.getGitAuth());
  }

  public TaskChainResponse queueK8sTask(K8sStepParameters k8sStepParameters, K8sDeployRequest k8sDeployRequest,
      Ambiance ambiance, InfrastructureOutcome infrastructure) {
    TaskData taskData =
        TaskData.builder()
            .parameters(new Object[] {k8sDeployRequest})
            .taskType(NGTaskType.K8S_COMMAND_TASK_NG.name())
            .timeout(NGTimeConversionHelper.convertTimeStringToMilliseconds(k8sStepParameters.getTimeout().getValue()))
            .async(true)
            .build();

    final TaskRequest taskRequest = prepareTaskRequest(ambiance, taskData, kryoSerializer,
        StepUtils.generateLogAbstractions(ambiance), TaskCategory.DELEGATE_TASK_V2, Collections.emptyList());

    return TaskChainResponse.builder().taskRequest(taskRequest).chainEnd(true).passThroughData(infrastructure).build();
  }

  public List<String> renderValues(Ambiance ambiance, List<String> valuesFileContents) {
    if (isEmpty(valuesFileContents)) {
      return Collections.emptyList();
    }

    return valuesFileContents.stream()
        .map(valuesFileContent -> engineExpressionService.renderExpression(ambiance, valuesFileContent))
        .collect(Collectors.toList());
  }

  public TaskChainResponse executeValuesFetchTask(Ambiance ambiance, K8sStepParameters k8sStepParameters,
      InfrastructureOutcome infrastructure, K8sManifestOutcome k8sManifestOutcome,
      List<ValuesManifestOutcome> aggregatedValuesManifests) {
    List<GitFetchFilesConfig> gitFetchFilesConfigs = new ArrayList<>();

    for (ValuesManifestOutcome valuesManifest : aggregatedValuesManifests) {
      if (ManifestStoreType.GIT.equals(valuesManifest.getStore().getStoreConfig().getKind())) {
        GitStore gitStore = (GitStore) valuesManifest.getStore().getStoreConfig();
        String connectorId = gitStore.getConnectorRef().getValue();
        ConnectorInfoDTO connectorDTO = getConnector(connectorId, ambiance);
        List<EncryptedDataDetail> encryptedDataDetails =
            getEncryptedDataDetails((GitConfigDTO) connectorDTO.getConnectorConfig(), ambiance);
        GitStoreDelegateConfig gitStoreDelegateConfig =
            getGitStoreDelegateConfig(gitStore, connectorDTO, encryptedDataDetails);

        gitFetchFilesConfigs.add(GitFetchFilesConfig.builder()
                                     .identifier(valuesManifest.getIdentifier())
                                     .succeedIfFileNotFound(false)
                                     .gitStoreDelegateConfig(gitStoreDelegateConfig)
                                     .build());
      }
    }

    String accountId = AmbianceHelper.getAccountId(ambiance);
    GitFetchRequest gitFetchRequest =
        GitFetchRequest.builder().gitFetchFilesConfigs(gitFetchFilesConfigs).accountId(accountId).build();

    final TaskData taskData =
        TaskData.builder()
            .async(true)
            .timeout(NGTimeConversionHelper.convertTimeStringToMilliseconds(k8sStepParameters.getTimeout().getValue()))
            .taskType(NGTaskType.GIT_FETCH_NEXT_GEN_TASK.name())
            .parameters(new Object[] {gitFetchRequest})
            .build();

    LinkedHashMap<String, String> logAbstractions = StepUtils.generateLogAbstractions(ambiance);
    String baseLoggingKey = LogHelper.generateLogBaseKey(logAbstractions);

    List<String> commandUnits =
        Arrays.asList(K8sCommandUnitConstants.FetchFiles, K8sCommandUnitConstants.Init, K8sCommandUnitConstants.Prepare,
            K8sCommandUnitConstants.Apply, K8sCommandUnitConstants.WaitForSteadyState, K8sCommandUnitConstants.WrapUp);
    LoggingMetadata loggingMetadata =
        LoggingMetadata.builder().baseLoggingKey(baseLoggingKey).commandUnits(commandUnits).build();

    final TaskRequest taskRequest = prepareTaskRequest(
        ambiance, taskData, kryoSerializer, logAbstractions, TaskCategory.DELEGATE_TASK_V2, commandUnits);

    K8sStepPassThroughData k8sStepPassThroughData = K8sStepPassThroughData.builder()
                                                        .k8sManifestOutcome(k8sManifestOutcome)
                                                        .valuesManifestOutcomes(aggregatedValuesManifests)
                                                        .infrastructure(infrastructure)
                                                        .build();
    return TaskChainResponse.builder()
        .chainEnd(false)
        .taskRequest(taskRequest)
        .passThroughData(k8sStepPassThroughData)
        .metadata(loggingMetadata)
        .build();
  }

  public TaskChainResponse startChainLink(
      K8sStepExecutor k8sStepExecutor, Ambiance ambiance, K8sStepParameters k8sStepParameters) {
    ServiceOutcome serviceOutcome = (ServiceOutcome) outcomeService.resolve(
        ambiance, RefObjectUtil.getOutcomeRefObject(OutcomeExpressionConstants.SERVICE));

    InfrastructureOutcome infrastructureOutcome = (InfrastructureOutcome) outcomeService.resolve(
        ambiance, RefObjectUtil.getOutcomeRefObject(OutcomeExpressionConstants.INFRASTRUCTURE));

    Map<String, ManifestOutcome> manifestOutcomeMap = serviceOutcome.getManifests();
    Validator.notEmptyCheck("Manifests can't be empty", manifestOutcomeMap.keySet());
    Validator.notEmptyCheck("Timeout cannot be empty", k8sStepParameters.getTimeout().getValue());

    K8sManifestOutcome k8sManifestOutcome = getK8sManifestOutcome(new LinkedList<>(manifestOutcomeMap.values()));
    List<ValuesManifestOutcome> aggregatedValuesManifests =
        getAggregatedValuesManifests(new LinkedList<>(manifestOutcomeMap.values()));

    if (isEmpty(aggregatedValuesManifests)) {
      return k8sStepExecutor.executeK8sTask(
          k8sManifestOutcome, ambiance, k8sStepParameters, Collections.emptyList(), infrastructureOutcome);
    }

    if (!isAnyRemoteStore(aggregatedValuesManifests)) {
      List<String> valuesFileContentsForLocalStore = getValuesFileContentsForLocalStore(aggregatedValuesManifests);
      return k8sStepExecutor.executeK8sTask(
          k8sManifestOutcome, ambiance, k8sStepParameters, valuesFileContentsForLocalStore, infrastructureOutcome);
    }

    return executeValuesFetchTask(
        ambiance, k8sStepParameters, infrastructureOutcome, k8sManifestOutcome, aggregatedValuesManifests);
  }

  @VisibleForTesting
  public K8sManifestOutcome getK8sManifestOutcome(@NotEmpty List<ManifestOutcome> manifestOutcomes) {
    List<ManifestOutcome> k8sManifests =
        manifestOutcomes.stream()
            .filter(manifestOutcome -> ManifestType.K8Manifest.equals(manifestOutcome.getType()))
            .collect(Collectors.toList());
    if (isEmpty(k8sManifests)) {
      throw new InvalidRequestException("K8s Manifests are mandatory for k8s Rolling step", WingsException.USER);
    }

    if (k8sManifests.size() > 1) {
      throw new InvalidRequestException("There can be only a single K8s manifest", WingsException.USER);
    }
    return (K8sManifestOutcome) k8sManifests.get(0);
  }

  @VisibleForTesting
  public List<ValuesManifestOutcome> getAggregatedValuesManifests(@NotEmpty List<ManifestOutcome> manifestOutcomeList) {
    List<ValuesManifestOutcome> aggregateValuesManifests = new ArrayList<>();

    List<ValuesManifestOutcome> serviceValuesManifests =
        manifestOutcomeList.stream()
            .filter(manifestOutcome -> ManifestType.VALUES.equals(manifestOutcome.getType()))
            .map(manifestOutcome -> (ValuesManifestOutcome) manifestOutcome)
            .collect(Collectors.toList());

    if (isNotEmpty(serviceValuesManifests)) {
      aggregateValuesManifests.addAll(serviceValuesManifests);
    }
    return aggregateValuesManifests;
  }

  private List<String> getValuesFileContentsForLocalStore(List<ValuesManifestOutcome> aggregatedValuesManifests) {
    // TODO: implement when local store is available
    return Collections.emptyList();
  }

  private boolean isAnyRemoteStore(@NotEmpty List<ValuesManifestOutcome> aggregatedValuesManifests) {
    return aggregatedValuesManifests.stream().anyMatch(
        valuesManifest -> ManifestStoreType.GIT.equals(valuesManifest.getStore().getStoreConfig().getKind()));
  }

  public TaskChainResponse executeNextLink(K8sStepExecutor k8sStepExecutor, Ambiance ambiance,
      K8sStepParameters k8sStepParameters, PassThroughData passThroughData, Map<String, ResponseData> responseDataMap) {
    GitFetchResponse gitFetchResponse = (GitFetchResponse) responseDataMap.values().iterator().next();

    if (gitFetchResponse.getTaskStatus() != TaskStatus.SUCCESS) {
      throw new InvalidRequestException(gitFetchResponse.getErrorMessage());
    }
    Map<String, FetchFilesResult> gitFetchFilesResultMap = gitFetchResponse.getFilesFromMultipleRepo();

    K8sStepPassThroughData k8sStepPassThroughData = (K8sStepPassThroughData) passThroughData;

    K8sManifestOutcome k8sManifest = k8sStepPassThroughData.getK8sManifestOutcome();
    List<ValuesManifestOutcome> valuesManifests = k8sStepPassThroughData.getValuesManifestOutcomes();

    List<String> valuesFileContents = getFileContents(gitFetchFilesResultMap, valuesManifests);

    return k8sStepExecutor.executeK8sTask(
        k8sManifest, ambiance, k8sStepParameters, valuesFileContents, k8sStepPassThroughData.getInfrastructure());
  }

  private List<String> getFileContents(
      Map<String, FetchFilesResult> gitFetchFilesResultMap, List<ValuesManifestOutcome> valuesManifests) {
    List<String> valuesFileContents = new ArrayList<>();

    for (ValuesManifestOutcome valuesManifest : valuesManifests) {
      if (ManifestStoreType.GIT.equals(valuesManifest.getStore().getStoreConfig().getKind())) {
        FetchFilesResult gitFetchFilesResult = gitFetchFilesResultMap.get(valuesManifest.getIdentifier());
        valuesFileContents.addAll(
            gitFetchFilesResult.getFiles().stream().map(GitFile::getFileContent).collect(Collectors.toList()));
      }
      // TODO: for local store, add files directly
    }
    return valuesFileContents;
  }
}
