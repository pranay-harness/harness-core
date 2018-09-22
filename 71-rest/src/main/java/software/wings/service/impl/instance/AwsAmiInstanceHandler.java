package software.wings.service.impl.instance;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.stream.Collectors.toList;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.inject.Singleton;

import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.WingsException;
import software.wings.annotation.Encryptable;
import software.wings.api.AmiStepExecutionSummary;
import software.wings.api.AwsAutoScalingGroupDeploymentInfo;
import software.wings.api.ContainerServiceData;
import software.wings.api.DeploymentInfo;
import software.wings.api.DeploymentSummary;
import software.wings.api.PhaseExecutionData;
import software.wings.api.PhaseStepExecutionData;
import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.AwsConfig;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.key.deployment.AwsAmiDeploymentKey;
import software.wings.beans.infrastructure.instance.key.deployment.DeploymentKey;
import software.wings.exception.HarnessException;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.sm.PhaseStepExecutionSummary;
import software.wings.sm.StepExecutionSummary;
import software.wings.utils.Validator;

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
public class AwsAmiInstanceHandler extends AwsInstanceHandler {
  @Override
  public void syncInstances(String appId, String infraMappingId) throws HarnessException {
    // Key - Auto scaling group with revision, Value - Instance
    Multimap<String, Instance> asgInstanceMap = ArrayListMultimap.create();
    syncInstancesInternal(appId, infraMappingId, asgInstanceMap, null, false);
  }

  private void syncInstancesInternal(String appId, String infraMappingId, Multimap<String, Instance> asgInstanceMap,
      List<DeploymentSummary> newDeploymentSummaries, boolean rollbak) throws HarnessException {
    Map<String, DeploymentSummary> asgNamesDeploymentSummaryMap = getDeploymentSummaryMap(newDeploymentSummaries);

    InfrastructureMapping infrastructureMapping = infraMappingService.get(appId, infraMappingId);
    Validator.notNullCheck("Infra mapping is null for id:" + infraMappingId, infrastructureMapping);
    if (!(infrastructureMapping instanceof AwsAmiInfrastructureMapping)) {
      String msg =
          "Incompatible infra mapping type. Expecting ami type. Found:" + infrastructureMapping.getInfraMappingType();
      logger.error(msg);
      throw new HarnessException(msg);
    }

    // key - ec2 instance id, value - instance
    Map<String, Instance> ec2InstanceIdInstanceMap = Maps.newHashMap();

    loadInstanceMapBasedOnType(appId, infraMappingId, asgInstanceMap, ec2InstanceIdInstanceMap);

    SettingAttribute cloudProviderSetting = settingsService.get(infrastructureMapping.getComputeProviderSettingId());
    AwsConfig awsConfig = (AwsConfig) cloudProviderSetting.getValue();
    List<EncryptedDataDetail> encryptedDataDetails =
        secretManager.getEncryptionDetails((Encryptable) cloudProviderSetting.getValue(), null, null);

    AwsAmiInfrastructureMapping amiInfraMapping = (AwsAmiInfrastructureMapping) infrastructureMapping;
    String region = amiInfraMapping.getRegion();

    handleEc2InstanceSync(ec2InstanceIdInstanceMap, awsConfig, encryptedDataDetails, region);

    handleAsgInstanceSync(region, asgInstanceMap, awsConfig, encryptedDataDetails, infrastructureMapping,
        asgNamesDeploymentSummaryMap, true, rollbak);
  }

  private Map<String, DeploymentSummary> getDeploymentSummaryMap(List<DeploymentSummary> newDeploymentSummaries) {
    if (EmptyPredicate.isEmpty(newDeploymentSummaries)) {
      return Collections.EMPTY_MAP;
    }

    Map<String, DeploymentSummary> deploymentSummaryMap = new HashMap<>();
    newDeploymentSummaries.forEach(deploymentSummary
        -> deploymentSummaryMap.put(
            ((AwsAutoScalingGroupDeploymentInfo) deploymentSummary.getDeploymentInfo()).getAutoScalingGroupName(),
            deploymentSummary));

    return deploymentSummaryMap;
  }

  @Override
  public void handleNewDeployment(List<DeploymentSummary> deploymentSummaries, boolean rollback)
      throws HarnessException {
    if (!(deploymentSummaries.iterator().next().getDeploymentInfo() instanceof AwsAutoScalingGroupDeploymentInfo)) {
      throw new HarnessException("Incompatible deployment type.");
    }

    Multimap<String, Instance> asgInstanceMap = ArrayListMultimap.create();

    deploymentSummaries.forEach(deploymentSummary
        -> asgInstanceMap.put(
            ((AwsAutoScalingGroupDeploymentInfo) deploymentSummary.getDeploymentInfo()).getAutoScalingGroupName(),
            null));

    syncInstancesInternal(deploymentSummaries.iterator().next().getAppId(),
        deploymentSummaries.iterator().next().getInfraMappingId(), asgInstanceMap, deploymentSummaries, rollback);
  }

  /**
   * Returns the auto scaling group names
   */
  private List<String> getASGFromAMIDeployment(PhaseExecutionData phaseExecutionData,
      PhaseStepExecutionData phaseStepExecutionData, WorkflowExecution workflowExecution) throws HarnessException {
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
        throw new HarnessException(
            "Step execution summary null for AMI Deploy Step for workflow: " + workflowExecution.getName());
      }

    } else {
      throw new HarnessException(
          "Phase step execution summary null for AMI Deploy for workflow: " + workflowExecution.getName());
    }

    return autoScalingGroupNames;
  }

  @Override
  public Optional<List<DeploymentInfo>> getDeploymentInfo(PhaseExecutionData phaseExecutionData,
      PhaseStepExecutionData phaseStepExecutionData, WorkflowExecution workflowExecution,
      InfrastructureMapping infrastructureMapping, String stateExecutionInstanceId, Artifact artifact)
      throws HarnessException {
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
      throw new WingsException("Invalid deploymentKey passed for AwsAmiDeploymentKey" + deploymentKey);
    }
  }
}
