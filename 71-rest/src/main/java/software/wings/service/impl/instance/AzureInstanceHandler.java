package software.wings.service.impl.instance;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.stream.Collectors.toSet;

import com.google.common.collect.Maps;
import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import software.wings.annotation.EncryptableSetting;
import software.wings.api.DeploymentInfo;
import software.wings.api.DeploymentSummary;
import software.wings.api.PhaseExecutionData;
import software.wings.api.PhaseStepExecutionData;
import software.wings.beans.AzureConfig;
import software.wings.beans.AzureInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.infrastructure.Host;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.info.HostInstanceInfo;
import software.wings.beans.infrastructure.instance.info.InstanceInfo;
import software.wings.beans.infrastructure.instance.key.deployment.DeploymentKey;
import software.wings.exception.HarnessException;
import software.wings.helpers.ext.azure.AzureHelperService;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.utils.Validator;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class AzureInstanceHandler extends InstanceHandler {
  @Inject protected AzureHelperService azureHelperService;

  @Override
  public void syncInstances(String appId, String infraMappingId) throws HarnessException {
    InfrastructureMapping infrastructureMapping = infraMappingService.get(appId, infraMappingId);
    Validator.notNullCheck("Infra mapping is null for id:" + infraMappingId, infrastructureMapping);

    if (!(infrastructureMapping instanceof AzureInfrastructureMapping)) {
      String msg = "Incompatible infra mapping type. Expecting AZURE_INFRA type. Found:"
          + infrastructureMapping.getInfraMappingType();
      logger.error(msg);
      throw new HarnessException(msg);
    }

    AzureInfrastructureMapping azureInfrastructureMapping = (AzureInfrastructureMapping) infrastructureMapping;
    Map<String, Instance> azureInstanceIdInstanceMap = Maps.newHashMap();

    loadInstanceMapBasedOnType(appId, infraMappingId, azureInstanceIdInstanceMap);

    logger.info("Found {} azure instances for app {} and infraMapping {} ",
        azureInstanceIdInstanceMap != null ? azureInstanceIdInstanceMap.size() : 0, appId, infraMappingId);

    SettingAttribute cloudProviderSetting = settingsService.get(infrastructureMapping.getComputeProviderSettingId());
    AzureConfig azureConfig = (AzureConfig) cloudProviderSetting.getValue();
    List<EncryptedDataDetail> encryptedDataDetails =
        secretManager.getEncryptionDetails((EncryptableSetting) cloudProviderSetting.getValue(), null, null);

    String subscriptionId = azureInfrastructureMapping.getSubscriptionId();

    if (azureInstanceIdInstanceMap != null && azureInstanceIdInstanceMap.size() > 0) {
      handleInstanceSync(azureInstanceIdInstanceMap, azureConfig, encryptedDataDetails, subscriptionId);
    }
  }

  protected void loadInstanceMapBasedOnType(
      String appId, String infraMappingId, Map<String, Instance> azureInstanceIdInstanceMap) {
    List<Instance> instanceList = getInstances(appId, infraMappingId);
    instanceList.forEach(instance -> {
      InstanceInfo instanceInfo = instance.getInstanceInfo();
      if (instanceInfo instanceof HostInstanceInfo) {
        HostInstanceInfo hostInstanceInfo = (HostInstanceInfo) instanceInfo;
        if (hostInstanceInfo != null) {
          String hostName = hostInstanceInfo.getHostName();
          azureInstanceIdInstanceMap.put(hostName, instance);
        }
      }
    });
  }

  protected void handleInstanceSync(Map<String, Instance> azureInstanceIdInstanceMap, AzureConfig azureConfig,
      List<EncryptedDataDetail> encryptedDataDetails, String subscriptionId) {
    if (azureInstanceIdInstanceMap.size() > 0) {
      AzureInfrastructureMapping azureInfrastructureMapping =
          AzureInfrastructureMapping.Builder.anAzureInfrastructureMapping().withSubscriptionId(subscriptionId).build();

      SettingAttribute settingAttribute = settingsService.get(azureInfrastructureMapping.getComputeProviderSettingId());

      List<Host> activeHostList = azureHelperService.listHosts(
          azureInfrastructureMapping, settingAttribute, encryptedDataDetails, new PageRequest<>());
      deleteRunningInstancesFromMap(azureInstanceIdInstanceMap, activeHostList);
    }
  }

  private void deleteRunningInstancesFromMap(
      Map<String, Instance> azureInstanceIdInstanceMap, List<Host> activeHostList) {
    Instance azureInstance = azureInstanceIdInstanceMap.values().iterator().next();
    logger.info(
        "Total no of Azure instances found in DB for InfraMappingId: {} and AppId: {}: {}, No of Running instances found in azure:{}",
        azureInstance.getInfraMappingId(), azureInstance.getAppId(), azureInstanceIdInstanceMap.size(),
        activeHostList.size());

    azureInstanceIdInstanceMap.keySet().removeAll(activeHostList.stream().map(Host::getHostName).collect(toSet()));

    Set<String> instanceIdsToBeDeleted = azureInstanceIdInstanceMap.entrySet()
                                             .stream()
                                             .map(entry -> entry.getValue().getUuid())
                                             .collect(Collectors.toSet());

    if (isNotEmpty(instanceIdsToBeDeleted)) {
      logger.info("Total no of Azure instances to be deleted for InfraMappingId: {}, AppId: {} : {}",
          azureInstance.getInfraMappingId(), azureInstance.getAppId(), instanceIdsToBeDeleted.size());
      instanceService.delete(instanceIdsToBeDeleted);
    }
  }

  @Override
  public void handleNewDeployment(List<DeploymentSummary> deploymentSummaries, boolean rollback) {}

  @Override
  public Optional<List<DeploymentInfo>> getDeploymentInfo(PhaseExecutionData phaseExecutionData,
      PhaseStepExecutionData phaseStepExecutionData, WorkflowExecution workflowExecution,
      InfrastructureMapping infrastructureMapping, String stateExecutionInstanceId, Artifact artifact)
      throws HarnessException {
    throw new HarnessException("Deployments should be handled at InstanceHelper for azure type.");
  }

  @Override
  public DeploymentKey generateDeploymentKey(DeploymentInfo deploymentInfo) {
    return null;
  }

  @Override
  protected void setDeploymentKey(DeploymentSummary deploymentSummary, DeploymentKey deploymentKey) {
    // Do Nothing
  }
}
