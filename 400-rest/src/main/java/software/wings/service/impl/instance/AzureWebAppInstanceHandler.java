package software.wings.service.impl.instance;

import static io.harness.beans.FeatureName.AZURE_WEBAPP;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.validation.Validator.notNullCheck;

import static com.google.common.collect.Sets.difference;
import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.beans.FeatureName;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.task.azure.AzureTaskExecutionResponse;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters;
import io.harness.delegate.task.azure.appservice.webapp.response.AzureAppDeploymentData;
import io.harness.delegate.task.azure.appservice.webapp.response.AzureWebAppListWebAppInstancesResponse;
import io.harness.exception.InvalidRequestException;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.api.AzureWebAppDeploymentInfo;
import software.wings.api.DeploymentInfo;
import software.wings.api.DeploymentSummary;
import software.wings.api.PhaseExecutionData;
import software.wings.api.PhaseStepExecutionData;
import software.wings.api.ondemandrollback.OnDemandRollbackInfo;
import software.wings.beans.AzureConfig;
import software.wings.beans.AzureWebAppInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.Instance.InstanceBuilder;
import software.wings.beans.infrastructure.instance.info.AzureWebAppInstanceInfo;
import software.wings.beans.infrastructure.instance.info.InstanceInfo;
import software.wings.beans.infrastructure.instance.key.deployment.AzureWebAppDeploymentKey;
import software.wings.beans.infrastructure.instance.key.deployment.DeploymentKey;
import software.wings.service.AzureVMSSInstanceSyncPerpetualTaskCreator;
import software.wings.service.InstanceSyncPerpetualTaskCreator;
import software.wings.service.intfc.azure.manager.AzureAppServiceManager;
import software.wings.sm.PhaseStepExecutionSummary;
import software.wings.sm.StepExecutionSummary;
import software.wings.sm.states.azure.appservices.AzureAppServiceSlotSetupExecutionSummary;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Slf4j
@Singleton
public class AzureWebAppInstanceHandler extends InstanceHandler implements InstanceSyncByPerpetualTaskHandler {
  @Inject private AzureAppServiceManager azureAppServiceManager;
  @Inject private AzureVMSSInstanceSyncPerpetualTaskCreator perpetualTaskCreator;

  @Override
  public void syncInstances(String appId, String infraMappingId, InstanceSyncFlow instanceSyncFlow) {
    AzureWebAppInfrastructureMapping infrastructureMapping = getInfraMapping(appId, infraMappingId);
    Multimap<String, Instance> appNameAndSlotNameToInstancesInDbMap = getCurrentInstancesInDb(appId, infraMappingId);
    if (isEmpty(appNameAndSlotNameToInstancesInDbMap.keySet())) {
      return;
    }
    appNameAndSlotNameToInstancesInDbMap.keys().forEach(appNameAndSlotName -> {
      Map<String, AzureAppDeploymentData> latestSlotWebAppInstances =
          getLatestSlotWebAppInstances(infrastructureMapping, appId, appNameAndSlotName);
      syncInstances(appNameAndSlotNameToInstancesInDbMap.get(appNameAndSlotName), latestSlotWebAppInstances, null,
          infrastructureMapping, instanceSyncFlow);
    });
  }

  private AzureWebAppInfrastructureMapping getInfraMapping(String appId, String infraMappingId) {
    InfrastructureMapping infrastructureMapping = infraMappingService.get(appId, infraMappingId);
    notNullCheck(format("Infra mapping is null for id: [%s]", infraMappingId), infrastructureMapping);
    if (!(infrastructureMapping instanceof AzureWebAppInfrastructureMapping)) {
      String msg = format("Incompatible infra mapping type. Expecting Azure Web App type. Found: [%s]",
          infrastructureMapping.getInfraMappingType());
      log.error(msg);
      throw new InvalidRequestException(msg);
    }
    return (AzureWebAppInfrastructureMapping) infrastructureMapping;
  }

  private Multimap<String, Instance> getCurrentInstancesInDb(String appId, String infraMappingId) {
    Multimap<String, Instance> appNameToInstancesMap = ArrayListMultimap.create();
    List<Instance> instances = getInstances(appId, infraMappingId);
    if (isNotEmpty(instances)) {
      instances.forEach(instance -> {
        InstanceInfo instanceInfo = instance.getInstanceInfo();
        if (instanceInfo instanceof AzureWebAppInstanceInfo) {
          AzureWebAppInstanceInfo azureWebAppInstanceInfo = (AzureWebAppInstanceInfo) instanceInfo;
          appNameToInstancesMap.put(getAppNameAndSlotNameKey(azureWebAppInstanceInfo), instance);
        }
      });
    }
    return appNameToInstancesMap;
  }

  private Map<String, AzureAppDeploymentData> getLatestSlotWebAppInstances(
      AzureWebAppInfrastructureMapping infrastructureMapping, String appId, String appNameAndSlotName) {
    SettingAttribute settingAttribute = settingsService.get(infrastructureMapping.getComputeProviderSettingId());
    notNullCheck("Cloud provider is null", settingAttribute);
    AzureConfig azureConfig = (AzureConfig) settingAttribute.getValue();
    List<EncryptedDataDetail> encryptedDataDetails = secretManager.getEncryptionDetails(azureConfig, null, null);
    String subscriptionId = infrastructureMapping.getSubscriptionId();
    String resourceGroupName = infrastructureMapping.getResourceGroup();
    String appName = appNameAndSlotName.split("_")[0];
    String slotName = appNameAndSlotName.split("_")[1];

    List<AzureAppDeploymentData> deploymentData =
        azureAppServiceManager.listWebAppInstances(azureConfig, encryptedDataDetails, appId, subscriptionId,
            resourceGroupName, AzureAppServiceTaskParameters.AzureAppServiceType.WEB_APP, appName, slotName);

    return deploymentData.stream().collect(
        Collectors.toMap(AzureAppDeploymentData::getInstanceId, Function.identity()));
  }

  @Override
  public void handleNewDeployment(
      List<DeploymentSummary> deploymentSummaries, boolean rollback, OnDemandRollbackInfo onDemandRollbackInfo) {
    if (isEmpty(deploymentSummaries)) {
      return;
    }

    String appId = deploymentSummaries.iterator().next().getAppId();
    String infraMappingId = deploymentSummaries.iterator().next().getInfraMappingId();
    AzureWebAppInfrastructureMapping infrastructureMapping = getInfraMapping(appId, infraMappingId);

    Multimap<String, Instance> appNameAndSlotNameToInstanceInDbMap = getCurrentInstancesInDb(appId, infraMappingId);

    Set<String> allSlotWebAppKeys = getAllSlotWebAppKeys(deploymentSummaries, appNameAndSlotNameToInstanceInDbMap);

    if (isNotEmpty(allSlotWebAppKeys)) {
      allSlotWebAppKeys.forEach(appNameAndSlotName -> {
        Map<String, AzureAppDeploymentData> latestSlotWebAppInstances =
            getLatestSlotWebAppInstances(infrastructureMapping, appId, appNameAndSlotName);

        DeploymentSummary eligibleDeploymentSummary =
            getEligibleDeploymentSummary(deploymentSummaries, appNameAndSlotName);

        syncInstances(appNameAndSlotNameToInstanceInDbMap.get(appNameAndSlotName), latestSlotWebAppInstances,
            eligibleDeploymentSummary, infrastructureMapping, InstanceSyncFlow.NEW_DEPLOYMENT);
      });
    }
  }

  @Override
  public void processInstanceSyncResponseFromPerpetualTask(
      InfrastructureMapping infrastructureMapping, DelegateResponseData response) {
    AzureTaskExecutionResponse azureTaskExecutionResponse = (AzureTaskExecutionResponse) response;
    AzureWebAppListWebAppInstancesResponse listWebAppInstancesResponse =
        (AzureWebAppListWebAppInstancesResponse) azureTaskExecutionResponse.getAzureTaskResponse();
    List<AzureAppDeploymentData> deploymentData = listWebAppInstancesResponse.getDeploymentData();
    if (deploymentData.isEmpty()) {
      return;
    }

    Map<String, AzureAppDeploymentData> latestSlotWebAppInstances =
        deploymentData.stream().collect(Collectors.toMap(AzureAppDeploymentData::getInstanceId, Function.identity()));
    String appNameAndSlotNameKey = getAppNameAndSlotNameKey(deploymentData.get(0));

    Multimap<String, Instance> appNameAndSlotNameToInstancesInDbMap =
        getCurrentInstancesInDb(infrastructureMapping.getAppId(), infrastructureMapping.getUuid());
    Collection<Instance> currentInstancesInDb = appNameAndSlotNameToInstancesInDbMap.get(appNameAndSlotNameKey);

    syncInstances(currentInstancesInDb, latestSlotWebAppInstances, null,
        (AzureWebAppInfrastructureMapping) infrastructureMapping, InstanceSyncFlow.PERPETUAL_TASK);
  }

  private void syncInstances(Collection<Instance> currentInstancesInDb,
      Map<String, AzureAppDeploymentData> latestSlotWebAppInstances, DeploymentSummary deploymentSummary,
      AzureWebAppInfrastructureMapping infrastructureMapping, InstanceSyncFlow instanceSyncFlow) {
    if (!canUpdateInstancesInDb(instanceSyncFlow, infrastructureMapping.getAccountId())) {
      return;
    }

    Map<String, Instance> latestSlotWebAppInstancesInDb = getInstanceIdToInstanceInDbMap(currentInstancesInDb);
    deleteOldInstances(latestSlotWebAppInstancesInDb, latestSlotWebAppInstances);
    Set<String> instanceIdsToBeAdded =
        difference(latestSlotWebAppInstances.keySet(), latestSlotWebAppInstancesInDb.keySet());

    if (isEmpty(instanceIdsToBeAdded)) {
      return;
    }

    Optional<DeploymentSummary> deploymentSummaryOp =
        getDeploymentSummary(deploymentSummary, latestSlotWebAppInstancesInDb, currentInstancesInDb);
    if (!deploymentSummaryOp.isPresent()) {
      log.warn("Couldn't find an instance from a previous deployment for inframapping: [{}]",
          infrastructureMapping.getUuid());
      return;
    }

    instanceIdsToBeAdded.forEach(instanceId -> {
      AzureAppDeploymentData webAppInstance = latestSlotWebAppInstances.get(instanceId);
      Instance instance = getInstance(infrastructureMapping, deploymentSummaryOp.get(), webAppInstance);
      instanceService.save(instance);
    });

    log.info("Instances to be added {}", instanceIdsToBeAdded.size());
  }

  private Map<String, Instance> getInstanceIdToInstanceInDbMap(Collection<Instance> currentInstancesInDb) {
    if (isEmpty(currentInstancesInDb)) {
      return emptyMap();
    }

    Map<String, Instance> vmIdToInstanceMap = new HashMap<>();
    currentInstancesInDb.forEach(instance -> {
      if (instance != null) {
        InstanceInfo instanceInfo = instance.getInstanceInfo();
        if (instanceInfo instanceof AzureWebAppInstanceInfo) {
          AzureWebAppInstanceInfo azureWebAppInstanceInfo = (AzureWebAppInstanceInfo) instanceInfo;
          vmIdToInstanceMap.put(azureWebAppInstanceInfo.getInstanceId(), instance);
        }
      }
    });
    return vmIdToInstanceMap;
  }

  private void deleteOldInstances(
      Map<String, Instance> instancesInDbMap, Map<String, AzureAppDeploymentData> latestDeployedInstancesMap) {
    Sets.SetView<String> webAppsToBeDeleted =
        difference(instancesInDbMap.keySet(), latestDeployedInstancesMap.keySet());
    Set<String> instanceIdsToBeDeleted = new HashSet<>();
    webAppsToBeDeleted.forEach(instanceKey -> {
      Instance instance = instancesInDbMap.get(instanceKey);
      if (instance != null) {
        instanceIdsToBeDeleted.add(instance.getUuid());
      }
    });
    if (isNotEmpty(instanceIdsToBeDeleted)) {
      instanceService.delete(instanceIdsToBeDeleted);
      log.info("Instances to be deleted {}", instanceIdsToBeDeleted.size());
    }
  }

  public Optional<DeploymentSummary> getDeploymentSummary(DeploymentSummary deploymentSummary,
      Map<String, Instance> latestSlotWebAppInstancesInDb, Collection<Instance> currentInstancesInDb) {
    if (deploymentSummary == null && isNotEmpty(latestSlotWebAppInstancesInDb)) {
      Optional<Instance> instanceWithExecutionInfoOptional = getInstanceWithExecutionInfo(currentInstancesInDb);
      return instanceWithExecutionInfoOptional.map(this::getDeploymentSummaryFromPrevious);
    } else {
      return Optional.of(getDeploymentSummaryForInstanceCreation(deploymentSummary, false));
    }
  }

  @NotNull
  private DeploymentSummary getDeploymentSummaryFromPrevious(Instance instanceWithExecutionInfo) {
    DeploymentSummary deploymentSummaryFromPrevious =
        DeploymentSummary.builder().deploymentInfo(AzureWebAppDeploymentInfo.builder().build()).build();
    generateDeploymentSummaryFromInstance(instanceWithExecutionInfo, deploymentSummaryFromPrevious);
    return deploymentSummaryFromPrevious;
  }

  private Instance getInstance(AzureWebAppInfrastructureMapping infrastructureMapping,
      DeploymentSummary finalDeploymentSummary, AzureAppDeploymentData webAppInstance) {
    InstanceBuilder instanceBuilder = buildInstanceBase(null, infrastructureMapping, finalDeploymentSummary);
    InstanceInfo instanceInfo = toAzureWebAppInstanceInfo(webAppInstance);
    instanceBuilder.instanceInfo(instanceInfo);
    return instanceBuilder.build();
  }

  private AzureWebAppInstanceInfo toAzureWebAppInstanceInfo(AzureAppDeploymentData webAppInstance) {
    return AzureWebAppInstanceInfo.builder()
        .instanceId(webAppInstance.getInstanceId())
        .slotName(webAppInstance.getDeploySlot())
        .slotId(webAppInstance.getDeploySlotId())
        .appName(webAppInstance.getAppName())
        .host(webAppInstance.getHostName())
        .appServicePlanId(webAppInstance.getAppServicePlanId())
        .instanceType(webAppInstance.getInstanceType())
        .state(webAppInstance.getInstanceState())
        .build();
  }

  @NotNull
  private Set<String> getAllSlotWebAppKeys(
      List<DeploymentSummary> deploymentSummaries, Multimap<String, Instance> appNameAndSlotNameToInstanceInDbMap) {
    Set<String> allSlotWebAppKeys = new HashSet<>();
    allSlotWebAppKeys.addAll(appNameAndSlotNameToInstanceInDbMap.keySet());
    allSlotWebAppKeys.addAll(deploymentSummaries.stream().map(this::getAppNameAndSlotNameKey).collect(toSet()));
    return allSlotWebAppKeys;
  }

  @Nullable
  private DeploymentSummary getEligibleDeploymentSummary(
      List<DeploymentSummary> deploymentSummaries, String appNameAndSlotName) {
    return deploymentSummaries.stream()
        .filter(summary -> appNameAndSlotName.equals(getAppNameAndSlotNameKey(summary)))
        .findFirst()
        .orElse(null);
  }

  private String getAppNameAndSlotNameKey(DeploymentSummary summary) {
    AzureWebAppDeploymentKey azureWebAppDeploymentKey = summary.getAzureWebAppDeploymentKey();
    return azureWebAppDeploymentKey.getKey();
  }

  @Override
  public FeatureName getFeatureFlagToEnablePerpetualTaskForInstanceSync() {
    return AZURE_WEBAPP;
  }

  @Override
  public InstanceSyncPerpetualTaskCreator getInstanceSyncPerpetualTaskCreator() {
    return perpetualTaskCreator;
  }

  @Override
  public Status getStatus(InfrastructureMapping infrastructureMapping, DelegateResponseData response) {
    AzureTaskExecutionResponse azureTaskExecutionResponse = (AzureTaskExecutionResponse) response;
    AzureWebAppListWebAppInstancesResponse listWebAppInstancesResponse =
        (AzureWebAppListWebAppInstancesResponse) azureTaskExecutionResponse.getAzureTaskResponse();
    List<AzureAppDeploymentData> deploymentData = listWebAppInstancesResponse.getDeploymentData();

    boolean success = SUCCESS == azureTaskExecutionResponse.getCommandExecutionStatus();
    boolean deleteTask = success && isEmpty(deploymentData);
    String errorMessage = success ? null : azureTaskExecutionResponse.getErrorMessage();
    return Status.builder().success(success).errorMessage(errorMessage).retryable(!deleteTask).build();
  }

  @Override
  public Optional<List<DeploymentInfo>> getDeploymentInfo(PhaseExecutionData phaseExecutionData,
      PhaseStepExecutionData phaseStepExecutionData, WorkflowExecution workflowExecution,
      InfrastructureMapping infrastructureMapping, String stateExecutionInstanceId, Artifact artifact) {
    PhaseStepExecutionSummary phaseStepExecutionSummary = phaseStepExecutionData.getPhaseStepExecutionSummary();
    if (phaseStepExecutionSummary != null) {
      Optional<StepExecutionSummary> stepExecutionSummaryOptional =
          phaseStepExecutionSummary.getStepExecutionSummaryList()
              .stream()
              .filter(stepExecutionSummary -> stepExecutionSummary instanceof AzureAppServiceSlotSetupExecutionSummary)
              .findFirst();
      if (stepExecutionSummaryOptional.isPresent()) {
        List<DeploymentInfo> deploymentInfoList = new ArrayList<>();
        AzureAppServiceSlotSetupExecutionSummary azureAppServiceSlotSetupExecutionSummary =
            (AzureAppServiceSlotSetupExecutionSummary) stepExecutionSummaryOptional.get();
        addNewWebInstanceToDeploymentList(deploymentInfoList, azureAppServiceSlotSetupExecutionSummary);
        addOldWebInstanceToDeploymentList(deploymentInfoList, azureAppServiceSlotSetupExecutionSummary);
        return of(deploymentInfoList);
      }
    }

    return Optional.empty();
  }

  private void addNewWebInstanceToDeploymentList(List<DeploymentInfo> deploymentInfoList,
      AzureAppServiceSlotSetupExecutionSummary azureAppServiceSlotSetupExecutionSummary) {
    String newAppName = azureAppServiceSlotSetupExecutionSummary.getNewAppName();
    String newSlotName = azureAppServiceSlotSetupExecutionSummary.getNewSlotName();
    if (isNotBlank(newAppName) && isNotBlank(newSlotName)) {
      deploymentInfoList.add(AzureWebAppDeploymentInfo.builder().appName(newAppName).slotName(newSlotName).build());
    }
  }

  private void addOldWebInstanceToDeploymentList(List<DeploymentInfo> deploymentInfoList,
      AzureAppServiceSlotSetupExecutionSummary azureAppServiceSlotSetupExecutionSummary) {
    String oldAppName = azureAppServiceSlotSetupExecutionSummary.getOldAppName();
    String oldSlotName = azureAppServiceSlotSetupExecutionSummary.getOldSlotName();
    if (isNotBlank(oldAppName) && isNotBlank(oldSlotName)) {
      deploymentInfoList.add(AzureWebAppDeploymentInfo.builder().appName(oldAppName).slotName(oldSlotName).build());
    }
  }

  @Override
  public DeploymentKey generateDeploymentKey(DeploymentInfo deploymentInfo) {
    AzureWebAppDeploymentInfo azureWebAppDeploymentInfo = (AzureWebAppDeploymentInfo) deploymentInfo;
    return AzureWebAppDeploymentKey.builder()
        .appName(azureWebAppDeploymentInfo.getAppName())
        .slotName(azureWebAppDeploymentInfo.getSlotName())
        .build();
  }

  @Override
  protected void setDeploymentKey(DeploymentSummary deploymentSummary, DeploymentKey deploymentKey) {
    if (deploymentKey instanceof AzureWebAppDeploymentKey) {
      deploymentSummary.setAzureWebAppDeploymentKey((AzureWebAppDeploymentKey) deploymentKey);
    } else {
      throw new InvalidRequestException(format(
          "Invalid deploymentKey passed for Azure Web App deployment: [%s]", deploymentKey.getClass().getName()));
    }
  }

  @Override
  public FeatureName getFeatureFlagToStopIteratorBasedInstanceSync() {
    return AZURE_WEBAPP;
  }

  @NotNull
  private String getAppNameAndSlotNameKey(AzureWebAppInstanceInfo azureWebAppInstanceInfo) {
    return format("%s_%s", azureWebAppInstanceInfo.getAppName(), azureWebAppInstanceInfo.getSlotName());
  }

  @NotNull
  private String getAppNameAndSlotNameKey(AzureAppDeploymentData azureAppDeploymentData) {
    return format("%s_%s", azureAppDeploymentData.getAppName(), azureAppDeploymentData.getDeploySlot());
  }
}
