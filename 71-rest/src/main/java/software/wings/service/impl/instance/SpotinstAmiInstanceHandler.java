package software.wings.service.impl.instance;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.difference;
import static com.google.common.collect.Sets.newHashSet;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.validation.Validator.notNullCheck;
import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.InvalidRequestException;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import software.wings.api.DeploymentInfo;
import software.wings.api.DeploymentSummary;
import software.wings.api.PhaseExecutionData;
import software.wings.api.PhaseStepExecutionData;
import software.wings.api.SpotinstAmiDeploymentInfo;
import software.wings.api.ondemandrollback.OnDemandRollbackInfo;
import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.AwsConfig;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SpotInstConfig;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.Instance.InstanceBuilder;
import software.wings.beans.infrastructure.instance.info.InstanceInfo;
import software.wings.beans.infrastructure.instance.info.SpotinstAmiInstanceInfo;
import software.wings.beans.infrastructure.instance.key.deployment.DeploymentKey;
import software.wings.beans.infrastructure.instance.key.deployment.SpotinstAmiDeploymentKey;
import software.wings.service.impl.spotinst.SpotinstHelperServiceManager;
import software.wings.sm.PhaseStepExecutionSummary;
import software.wings.sm.StepExecutionSummary;
import software.wings.sm.states.spotinst.SpotinstDeployExecutionSummary;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Singleton
public class SpotinstAmiInstanceHandler extends InstanceHandler {
  @Inject private SpotinstHelperServiceManager spotinstHelperServiceManager;

  @Override
  public void syncInstances(String appId, String infraMappingId) {
    AwsAmiInfrastructureMapping infrastructureMapping = getInfraMapping(appId, infraMappingId);
    SettingAttribute spotinstSettingAttribute = settingsService.get(infrastructureMapping.getSpotinstCloudProvider());
    SpotInstConfig spotinstConfig = (SpotInstConfig) spotinstSettingAttribute.getValue();
    List<EncryptedDataDetail> spotinstEncryptedDataDetails =
        secretManager.getEncryptionDetails(spotinstConfig, null, null);
    String region = infrastructureMapping.getRegion();
    SettingAttribute awsSettingAttribute = settingsService.get(infrastructureMapping.getComputeProviderSettingId());
    AwsConfig awsConfig = (AwsConfig) awsSettingAttribute.getValue();
    List<EncryptedDataDetail> awsEncryptedDataDetails = secretManager.getEncryptionDetails(awsConfig, null, null);

    Multimap<String, Instance> elastigroupIdToInstancesInDbMap = getCurrentInstancesInDb(appId, infraMappingId);
    Set<String> elastigroupIds = elastigroupIdToInstancesInDbMap.keySet();
    if (isEmpty(elastigroupIds)) {
      return;
    }

    elastigroupIds.forEach(elastigroupId -> {
      syncInstancesForElastigroup(elastigroupId, awsConfig, awsEncryptedDataDetails, spotinstConfig,
          spotinstEncryptedDataDetails, region, elastigroupIdToInstancesInDbMap.get(elastigroupId), false, appId, null,
          infrastructureMapping);
    });
  }

  @Override
  public void handleNewDeployment(
      List<DeploymentSummary> deploymentSummaries, boolean rollback, OnDemandRollbackInfo onDemandRollbackInfo) {
    if (isEmpty(deploymentSummaries)) {
      return;
    }

    String appId = deploymentSummaries.iterator().next().getAppId();
    String infraMappingId = deploymentSummaries.iterator().next().getInfraMappingId();
    Multimap<String, Instance> elastigroupIdToInstancesInDbMap = getCurrentInstancesInDb(appId, infraMappingId);

    AwsAmiInfrastructureMapping infrastructureMapping = getInfraMapping(appId, infraMappingId);
    SettingAttribute awsSettingAttribute = settingsService.get(infrastructureMapping.getComputeProviderSettingId());
    AwsConfig awsConfig = (AwsConfig) awsSettingAttribute.getValue();
    List<EncryptedDataDetail> awsEncryptedDataDetails = secretManager.getEncryptionDetails(awsConfig, null, null);
    SettingAttribute spotinstSettingAttribute = settingsService.get(infrastructureMapping.getSpotinstCloudProvider());
    SpotInstConfig spotinstConfig = (SpotInstConfig) spotinstSettingAttribute.getValue();
    List<EncryptedDataDetail> spotinstEncryptedDataDetails =
        secretManager.getEncryptionDetails(spotinstConfig, null, null);
    String region = infrastructureMapping.getRegion();

    Set<String> allElastigroupIds = newHashSet();
    allElastigroupIds.addAll(elastigroupIdToInstancesInDbMap.keySet());
    allElastigroupIds.addAll(deploymentSummaries.stream()
                                 .map(summary -> summary.getSpotinstAmiDeploymentKey().getElastigroupId())
                                 .collect(toSet()));
    if (isNotEmpty(allElastigroupIds)) {
      allElastigroupIds.forEach(elastigroupId
          -> syncInstancesForElastigroup(elastigroupId, awsConfig, awsEncryptedDataDetails, spotinstConfig,
              spotinstEncryptedDataDetails, region, elastigroupIdToInstancesInDbMap.get(elastigroupId), rollback, appId,
              deploymentSummaries.stream()
                  .filter(summary -> summary.getSpotinstAmiDeploymentKey().getElastigroupId().equals(elastigroupId))
                  .findFirst()
                  .orElse(null),
              infrastructureMapping));
    }
  }

  @VisibleForTesting
  void syncInstancesForElastigroup(String elastigroupId, AwsConfig awsConfig,
      List<EncryptedDataDetail> awsEncryptedDataDetails, SpotInstConfig spotInstConfig,
      List<EncryptedDataDetail> spotinstEncryptedDataDetails, String region, Collection<Instance> currentInstancesInDb,
      boolean rollback, String appId, DeploymentSummary deploymentSummary,
      AwsAmiInfrastructureMapping infrastructureMapping) {
    Map<String, Instance> ec2InstanceIdToInstanceInDbMap = getEc2InstanceIdToInstanceInDbMap(currentInstancesInDb);
    Map<String, com.amazonaws.services.ec2.model.Instance> latestEc2InstanceIdToEc2InstanceMap =
        getLatestEc2InstanceIdToEc2InstanceMap(elastigroupId, awsConfig, awsEncryptedDataDetails, spotInstConfig,
            spotinstEncryptedDataDetails, region, appId);

    handleEc2InstanceDelete(ec2InstanceIdToInstanceInDbMap, latestEc2InstanceIdToEc2InstanceMap);

    Set<String> ec2InstanceIdsToBeAdded =
        difference(latestEc2InstanceIdToEc2InstanceMap.keySet(), ec2InstanceIdToInstanceInDbMap.keySet());
    if (isNotEmpty(ec2InstanceIdsToBeAdded)) {
      DeploymentSummary finalDeploymentSummary;
      if (deploymentSummary == null && isNotEmpty(currentInstancesInDb)) {
        Optional<Instance> instanceWithExecutionInfoOptional = getInstanceWithExecutionInfo(currentInstancesInDb);
        if (!instanceWithExecutionInfoOptional.isPresent()) {
          logger.warn("Couldn't find an instance from a previous deployment for inframapping: [{}]",
              infrastructureMapping.getUuid());
          return;
        }
        DeploymentSummary deploymentSummaryFromPrevious =
            DeploymentSummary.builder().deploymentInfo(SpotinstAmiDeploymentInfo.builder().build()).build();
        generateDeploymentSummaryFromInstance(instanceWithExecutionInfoOptional.get(), deploymentSummaryFromPrevious);
        finalDeploymentSummary = deploymentSummaryFromPrevious;
      } else {
        finalDeploymentSummary = getDeploymentSummaryForInstanceCreation(deploymentSummary, rollback);
      }

      ec2InstanceIdsToBeAdded.forEach(ec2InstanceId -> {
        com.amazonaws.services.ec2.model.Instance ec2Instance = latestEc2InstanceIdToEc2InstanceMap.get(ec2InstanceId);
        InstanceBuilder instanceBuilder = buildInstanceBase(null, infrastructureMapping, finalDeploymentSummary);
        String privateDnsName = buildHostInstanceKey(ec2Instance, infrastructureMapping.getUuid(), instanceBuilder);
        InstanceInfo instanceInfo = SpotinstAmiInstanceInfo.builder()
                                        .ec2Instance(ec2Instance)
                                        .elastigroupId(elastigroupId)
                                        .hostPublicDns(ec2Instance.getPublicDnsName())
                                        .hostName(privateDnsName)
                                        .build();
        instanceBuilder.instanceInfo(instanceInfo);
        Instance instance = instanceBuilder.build();
        instanceService.save(instance);
      });

      logger.info("Instances to be added {}", ec2InstanceIdsToBeAdded.size());
    }
  }

  private Map<String, Instance> getEc2InstanceIdToInstanceInDbMap(Collection<Instance> currentInstancesInDb) {
    if (isEmpty(currentInstancesInDb)) {
      return emptyMap();
    }

    Map<String, Instance> ec2InstanceIdToInstanceMap = newHashMap();
    currentInstancesInDb.forEach(instance -> {
      if (instance != null) {
        SpotinstAmiInstanceInfo instanceInfo = (SpotinstAmiInstanceInfo) instance.getInstanceInfo();
        String ec2InstanceId = instanceInfo.getEc2Instance().getInstanceId();
        ec2InstanceIdToInstanceMap.put(ec2InstanceId, instance);
      }
    });
    return ec2InstanceIdToInstanceMap;
  }

  private Map<String, com.amazonaws.services.ec2.model.Instance> getLatestEc2InstanceIdToEc2InstanceMap(
      String elastigroupId, AwsConfig awsConfig, List<EncryptedDataDetail> awsEncryptedDataDetails,
      SpotInstConfig spotInstConfig, List<EncryptedDataDetail> spotinstEncryptedDataDetails, String region,
      String appId) {
    List<com.amazonaws.services.ec2.model.Instance> instances = spotinstHelperServiceManager.listElastigroupInstances(
        spotInstConfig, spotinstEncryptedDataDetails, awsConfig, awsEncryptedDataDetails, region, appId, elastigroupId);
    if (isEmpty(instances)) {
      return emptyMap();
    }
    return instances.stream().collect(toMap(com.amazonaws.services.ec2.model.Instance::getInstanceId, identity()));
  }

  private AwsAmiInfrastructureMapping getInfraMapping(String appId, String infraMappingId) {
    InfrastructureMapping infrastructureMapping = infraMappingService.get(appId, infraMappingId);
    notNullCheck(format("Infra mapping is null for id: [%s]", infraMappingId), infrastructureMapping);
    if (!(infrastructureMapping instanceof AwsAmiInfrastructureMapping)) {
      String msg = format("Incompatible infra mapping type. Expecting ami type. Found: [%s]",
          infrastructureMapping.getInfraMappingType());
      logger.error(msg);
      throw new InvalidRequestException(msg);
    }
    return (AwsAmiInfrastructureMapping) infrastructureMapping;
  }

  private Multimap<String, Instance> getCurrentInstancesInDb(String appId, String infraMappingId) {
    Multimap<String, Instance> elastigroupIdToInstancesMap = ArrayListMultimap.create();
    List<Instance> instances = getInstances(appId, infraMappingId);
    if (isNotEmpty(instances)) {
      instances.forEach(instance -> {
        InstanceInfo instanceInfo = instance.getInstanceInfo();
        if (instanceInfo instanceof SpotinstAmiInstanceInfo) {
          SpotinstAmiInstanceInfo spotinstAmiInstanceInfo = (SpotinstAmiInstanceInfo) instanceInfo;
          elastigroupIdToInstancesMap.put(spotinstAmiInstanceInfo.getElastigroupId(), instance);
        }
      });
    }
    return elastigroupIdToInstancesMap;
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
              .filter(stepExecutionSummary -> stepExecutionSummary instanceof SpotinstDeployExecutionSummary)
              .findFirst();
      if (stepExecutionSummaryOptional.isPresent()) {
        SpotinstDeployExecutionSummary summary = (SpotinstDeployExecutionSummary) stepExecutionSummaryOptional.get();
        List<DeploymentInfo> infos = newArrayList();
        if (isNotEmpty(summary.getOldElastigroupId())) {
          infos.add(SpotinstAmiDeploymentInfo.builder()
                        .elastigroupId(summary.getOldElastigroupId())
                        .elastigroupName(summary.getOldElastigroupName())
                        .build());
        }
        if (isNotEmpty(summary.getNewElastigroupId())) {
          infos.add(SpotinstAmiDeploymentInfo.builder()
                        .elastigroupId(summary.getNewElastigroupId())
                        .elastigroupName(summary.getNewElastigroupName())
                        .build());
        }
        return Optional.of(infos);
      } else {
        throw new InvalidRequestException(
            format("Step execution summary null for AMI Spotinst Deploy Step for workflow: [%s]",
                workflowExecution.normalizedName()));
      }
    } else {
      throw new InvalidRequestException(
          format("Phase step execution summary null for AMI Spotinst Deploy for workflow: [%s]",
              workflowExecution.normalizedName()));
    }
  }

  @Override
  public DeploymentKey generateDeploymentKey(DeploymentInfo deploymentInfo) {
    SpotinstAmiDeploymentInfo spotinstAmiDeploymentInfo = (SpotinstAmiDeploymentInfo) deploymentInfo;
    return SpotinstAmiDeploymentKey.builder()
        .elastigroupId(spotinstAmiDeploymentInfo.getElastigroupId())
        .elastigroupName(spotinstAmiDeploymentInfo.getElastigroupName())
        .build();
  }

  @Override
  protected void setDeploymentKey(DeploymentSummary deploymentSummary, DeploymentKey deploymentKey) {
    if (deploymentKey instanceof SpotinstAmiDeploymentKey) {
      deploymentSummary.setSpotinstAmiDeploymentKey((SpotinstAmiDeploymentKey) deploymentKey);
    } else {
      throw new InvalidRequestException(
          format("Invalid deploymentKey passed for AwsAmiDeploymentKey: [%s]", deploymentKey.getClass().getName()));
    }
  }
}