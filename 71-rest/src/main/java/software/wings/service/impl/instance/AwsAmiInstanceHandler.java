package software.wings.service.impl.instance;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.validation.Validator.notNullCheck;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.FeatureName.STOP_INSTANCE_SYNC_VIA_ITERATOR_FOR_AWS_AMI_DEPLOYMENTS;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.inject.Singleton;

import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import software.wings.annotation.EncryptableSetting;
import software.wings.api.AmiStepExecutionSummary;
import software.wings.api.AwsAutoScalingGroupDeploymentInfo;
import software.wings.api.ContainerServiceData;
import software.wings.api.DeploymentInfo;
import software.wings.api.DeploymentSummary;
import software.wings.api.PhaseExecutionData;
import software.wings.api.PhaseStepExecutionData;
import software.wings.api.ondemandrollback.OnDemandRollbackInfo;
import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.AwsConfig;
import software.wings.beans.FeatureName;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.key.deployment.AwsAmiDeploymentKey;
import software.wings.beans.infrastructure.instance.key.deployment.DeploymentKey;
import software.wings.sm.PhaseStepExecutionSummary;
import software.wings.sm.StepExecutionSummary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author rktummala on 02/02/18
 */
@Singleton
@Slf4j
public class AwsAmiInstanceHandler extends AwsInstanceHandler {
  @Override
  public void syncInstances(String appId, String infraMappingId, InstanceSyncFlow instanceSyncFlow) {
    // Key - Auto scaling group with revision, Value - Instance
    Multimap<String, Instance> asgInstanceMap = ArrayListMultimap.create();
    syncInstancesInternal(appId, infraMappingId, asgInstanceMap, null, false);
  }

  private void syncInstancesInternal(String appId, String infraMappingId, Multimap<String, Instance> asgInstanceMap,
      List<DeploymentSummary> newDeploymentSummaries, boolean rollbak) {
    Map<String, DeploymentSummary> asgNamesDeploymentSummaryMap = getDeploymentSummaryMap(newDeploymentSummaries);

    InfrastructureMapping infrastructureMapping = infraMappingService.get(appId, infraMappingId);
    notNullCheck("Infra mapping is null for id:" + infraMappingId, infrastructureMapping);
    if (!(infrastructureMapping instanceof AwsAmiInfrastructureMapping)) {
      String msg =
          "Incompatible infra mapping type. Expecting ami type. Found:" + infrastructureMapping.getInfraMappingType();
      logger.error(msg);
      throw WingsException.builder().message(msg).build();
    }

    // key - ec2 instance id, value - instance
    Map<String, Instance> ec2InstanceIdInstanceMap = new HashMap<>();

    loadInstanceMapBasedOnType(appId, infraMappingId, asgInstanceMap, ec2InstanceIdInstanceMap);

    SettingAttribute cloudProviderSetting = settingsService.get(infrastructureMapping.getComputeProviderSettingId());
    AwsConfig awsConfig = (AwsConfig) cloudProviderSetting.getValue();
    List<EncryptedDataDetail> encryptedDataDetails =
        secretManager.getEncryptionDetails((EncryptableSetting) cloudProviderSetting.getValue(), null, null);

    AwsAmiInfrastructureMapping amiInfraMapping = (AwsAmiInfrastructureMapping) infrastructureMapping;
    String region = amiInfraMapping.getRegion();

    handleEc2InstanceSync(ec2InstanceIdInstanceMap, awsConfig, encryptedDataDetails, region);

    handleAsgInstanceSync(region, asgInstanceMap, awsConfig, encryptedDataDetails, infrastructureMapping,
        asgNamesDeploymentSummaryMap, true, rollbak);
  }

  private Map<String, DeploymentSummary> getDeploymentSummaryMap(List<DeploymentSummary> newDeploymentSummaries) {
    if (EmptyPredicate.isEmpty(newDeploymentSummaries)) {
      return Collections.emptyMap();
    }

    Map<String, DeploymentSummary> deploymentSummaryMap = new HashMap<>();
    newDeploymentSummaries.forEach(deploymentSummary
        -> deploymentSummaryMap.put(
            ((AwsAutoScalingGroupDeploymentInfo) deploymentSummary.getDeploymentInfo()).getAutoScalingGroupName(),
            deploymentSummary));

    return deploymentSummaryMap;
  }

  @Override
  public void handleNewDeployment(
      List<DeploymentSummary> deploymentSummaries, boolean rollback, OnDemandRollbackInfo onDemandRollbackInfo) {
    if (!(deploymentSummaries.iterator().next().getDeploymentInfo() instanceof AwsAutoScalingGroupDeploymentInfo)) {
      throw WingsException.builder().message("Incompatible deployment type.").build();
    }

    Multimap<String, Instance> asgInstanceMap = ArrayListMultimap.create();

    deploymentSummaries.forEach(deploymentSummary
        -> asgInstanceMap.put(
            ((AwsAutoScalingGroupDeploymentInfo) deploymentSummary.getDeploymentInfo()).getAutoScalingGroupName(),
            null));

    syncInstancesInternal(deploymentSummaries.iterator().next().getAppId(),
        deploymentSummaries.iterator().next().getInfraMappingId(), asgInstanceMap, deploymentSummaries, rollback);
  }

  @Override
  public FeatureName getFeatureFlagToStopIteratorBasedInstanceSync() {
    return STOP_INSTANCE_SYNC_VIA_ITERATOR_FOR_AWS_AMI_DEPLOYMENTS;
  }

  /**
   * Returns the auto scaling group names
   */
  private List<String> getASGFromAMIDeployment(PhaseExecutionData phaseExecutionData,
      PhaseStepExecutionData phaseStepExecutionData, WorkflowExecution workflowExecution) {
    List<String> autoScalingGroupNames = Lists.newArrayList();

    PhaseStepExecutionSummary phaseStepExecutionSummary = phaseStepExecutionData.getPhaseStepExecutionSummary();

    if (phaseStepExecutionSummary != null) {
      Optional<StepExecutionSummary> stepExecutionSummaryOptional =
          phaseStepExecutionSummary.getStepExecutionSummaryList()
              .stream()
              .filter(stepExecutionSummary -> stepExecutionSummary instanceof AmiStepExecutionSummary)
              .findFirst();

      if (stepExecutionSummaryOptional.isPresent()) {
        StepExecutionSummary stepExecutionSummary = stepExecutionSummaryOptional.get();

        AmiStepExecutionSummary amiStepExecutionSummary = (AmiStepExecutionSummary) stepExecutionSummary;

        // Capture the instances of the new revision
        if (isNotEmpty(amiStepExecutionSummary.getNewInstanceData())) {
          List<String> asgList = amiStepExecutionSummary.getNewInstanceData()
                                     .stream()
                                     .map(ContainerServiceData::getName)
                                     .collect(toList());
          if (isNotEmpty(asgList)) {
            autoScalingGroupNames.addAll(asgList);
          }
        }

        // Capture the instances of the old revision, note that the downsize operation need not bring the count
        // to zero.
        if (isNotEmpty(amiStepExecutionSummary.getOldInstanceData())) {
          List<String> asgList = amiStepExecutionSummary.getOldInstanceData()
                                     .stream()
                                     .map(ContainerServiceData::getName)
                                     .collect(toList());
          if (isNotEmpty(asgList)) {
            autoScalingGroupNames.addAll(asgList);
          }
        }
      } else {
        throw WingsException.builder()
            .message(
                "Step execution summary null for AMI Deploy Step for workflow: " + workflowExecution.normalizedName())
            .build();
      }
    } else {
      throw WingsException.builder()
          .message(
              "Phase step execution summary null for AMI Deploy for workflow: " + workflowExecution.normalizedName())
          .build();
    }

    return autoScalingGroupNames;
  }

  @Override
  public Optional<List<DeploymentInfo>> getDeploymentInfo(PhaseExecutionData phaseExecutionData,
      PhaseStepExecutionData phaseStepExecutionData, WorkflowExecution workflowExecution,
      InfrastructureMapping infrastructureMapping, String stateExecutionInstanceId, Artifact artifact) {
    List<String> autoScalingGroupNames =
        getASGFromAMIDeployment(phaseExecutionData, phaseStepExecutionData, workflowExecution);
    List<DeploymentInfo> deploymentInfos = new ArrayList<>();
    autoScalingGroupNames.forEach(autoScalingGroupName
        -> deploymentInfos.add(
            AwsAutoScalingGroupDeploymentInfo.builder().autoScalingGroupName(autoScalingGroupName).build()));
    return Optional.of(deploymentInfos);
  }

  @Override
  public DeploymentKey generateDeploymentKey(DeploymentInfo deploymentInfo) {
    return AwsAmiDeploymentKey.builder()
        .autoScalingGroupName(((AwsAutoScalingGroupDeploymentInfo) deploymentInfo).getAutoScalingGroupName())
        .build();
  }

  @Override
  protected void setDeploymentKey(DeploymentSummary deploymentSummary, DeploymentKey deploymentKey) {
    if (deploymentKey instanceof AwsAmiDeploymentKey) {
      deploymentSummary.setAwsAmiDeploymentKey((AwsAmiDeploymentKey) deploymentKey);
    } else {
      throw WingsException.builder()
          .message("Invalid deploymentKey passed for AwsAmiDeploymentKey" + deploymentKey)
          .build();
    }
  }
}
