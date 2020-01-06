package software.wings.service.impl.instance;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.validation.Validator.notNullCheck;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toSet;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import software.wings.annotation.EncryptableSetting;
import software.wings.api.AwsAutoScalingGroupDeploymentInfo;
import software.wings.api.DeploymentInfo;
import software.wings.api.DeploymentSummary;
import software.wings.api.DeploymentType;
import software.wings.api.PhaseExecutionData;
import software.wings.api.PhaseStepExecutionData;
import software.wings.api.ondemandrollback.OnDemandRollbackInfo;
import software.wings.beans.AwsConfig;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.Instance.InstanceBuilder;
import software.wings.beans.infrastructure.instance.info.AutoScalingGroupInstanceInfo;
import software.wings.beans.infrastructure.instance.info.Ec2InstanceInfo;
import software.wings.beans.infrastructure.instance.info.InstanceInfo;
import software.wings.beans.infrastructure.instance.key.deployment.DeploymentKey;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.impl.AwsInfrastructureProvider;
import software.wings.service.intfc.aws.manager.AwsAsgHelperServiceManager;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author rktummala on 02/01/18
 */
@Singleton
@Slf4j
public class AwsInstanceHandler extends InstanceHandler {
  @Inject protected AwsHelperService awsHelperService;
  @Inject private AwsAsgHelperServiceManager awsAsgHelperServiceManager;
  @Inject protected AwsInfrastructureProvider awsInfrastructureProvider;

  @Override
  public void syncInstances(String appId, String infraMappingId) {
    // Key - Auto scaling group with revision, Value - Instance
    Multimap<String, Instance> asgInstanceMap = ArrayListMultimap.create();

    InfrastructureMapping infrastructureMapping = infraMappingService.get(appId, infraMappingId);
    notNullCheck("Infra mapping is null for id:" + infraMappingId, infrastructureMapping);

    if (!(infrastructureMapping instanceof AwsInfrastructureMapping)) {
      String msg =
          "Incompatible infra mapping type. Expecting aws type. Found:" + infrastructureMapping.getInfraMappingType();
      logger.error(msg);
      throw WingsException.builder().message(msg).build();
    }

    AwsInfrastructureMapping awsInfraMapping = (AwsInfrastructureMapping) infrastructureMapping;

    // key - ec2 instance id, value - instance
    Map<String, Instance> ec2InstanceIdInstanceMap = Maps.newHashMap();

    loadInstanceMapBasedOnType(appId, infraMappingId, asgInstanceMap, ec2InstanceIdInstanceMap);

    SettingAttribute cloudProviderSetting = settingsService.get(infrastructureMapping.getComputeProviderSettingId());
    AwsConfig awsConfig = (AwsConfig) cloudProviderSetting.getValue();
    List<EncryptedDataDetail> encryptedDataDetails =
        secretManager.getEncryptionDetails((EncryptableSetting) cloudProviderSetting.getValue(), null, null);

    String region = awsInfraMapping.getRegion();

    // Check if the instances are still running. These instances were either the ones that were stored with the old
    // schema or the instances created using aws infra mapping with filter.
    if (ec2InstanceIdInstanceMap.size() > 0) {
      if (awsInfraMapping.getAwsInstanceFilter() != null) {
        handleEc2InstanceSyncWithAwsInfraMapping(
            ec2InstanceIdInstanceMap, awsConfig, encryptedDataDetails, region, awsInfraMapping);
      } else {
        handleEc2InstanceSync(ec2InstanceIdInstanceMap, awsConfig, encryptedDataDetails, region);
      }
    }

    handleAsgInstanceSync(
        region, asgInstanceMap, awsConfig, encryptedDataDetails, infrastructureMapping, null, true, false);
  }

  @Override
  public void handleNewDeployment(
      List<DeploymentSummary> deploymentSummaries, boolean rollback, OnDemandRollbackInfo onDemandRollbackInfo) {
    // All the new deployments are either handled at ASGInstanceHandler(for Aws ssh with asg) or InstanceHelper (for Aws
    // ssh with or without filter)
    throw WingsException.builder()
        .message("Deployments should be handled at InstanceHelper for aws ssh type except for with ASG.")
        .build();
  }

  @Override
  public Optional<List<DeploymentInfo>> getDeploymentInfo(PhaseExecutionData phaseExecutionData,
      PhaseStepExecutionData phaseStepExecutionData, WorkflowExecution workflowExecution,
      InfrastructureMapping infrastructureMapping, String stateExecutionInstanceId, Artifact artifact) {
    // All the new deployments are either handled at ASGInstanceHandler(for Aws ssh with asg) or InstanceHelper (for Aws
    // ssh with or without filter)
    throw WingsException.builder()
        .message("Deployments should be handled at InstanceHelper for aws ssh type except for with ASG.")
        .build();
  }

  @Override
  public DeploymentKey generateDeploymentKey(DeploymentInfo deploymentInfo) {
    return null;
  }

  @Override
  protected void setDeploymentKey(DeploymentSummary deploymentSummary, DeploymentKey deploymentKey) {
    // Do Nothing
  }

  protected void loadInstanceMapBasedOnType(String appId, String infraMappingId,
      Multimap<String, Instance> asgInstanceMap, Map<String, Instance> ec2InstanceIdInstanceMap) {
    List<Instance> instanceList = getInstances(appId, infraMappingId);
    instanceList.forEach(instance -> {
      InstanceInfo instanceInfo = instance.getInstanceInfo();
      if (instanceInfo instanceof AutoScalingGroupInstanceInfo) {
        AutoScalingGroupInstanceInfo asgInstanceInfo = (AutoScalingGroupInstanceInfo) instanceInfo;
        asgInstanceMap.put(asgInstanceInfo.getAutoScalingGroupName(), instance);
      } else if (instanceInfo instanceof Ec2InstanceInfo) {
        Ec2InstanceInfo ec2InstanceInfo = (Ec2InstanceInfo) instanceInfo;
        com.amazonaws.services.ec2.model.Instance ec2Instance = ec2InstanceInfo.getEc2Instance();
        if (ec2Instance != null) {
          String ec2InstanceId = ec2Instance.getInstanceId();
          ec2InstanceIdInstanceMap.put(ec2InstanceId, instance);
        }
      }
    });
  }

  protected void handleAsgInstanceSync(String region, Multimap<String, Instance> asgInstanceMap, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptedDataDetails, InfrastructureMapping infrastructureMapping,
      Map<String, DeploymentSummary> asgNameDeploymentSummaryMap, boolean isAmi, boolean rollback) {
    // This is to handle the case of the instances stored in the new schema.
    if (asgInstanceMap.size() > 0) {
      asgInstanceMap.keySet().forEach(autoScalingGroupName -> {
        List<com.amazonaws.services.ec2.model.Instance> latestEc2Instances = getEc2InstancesFromAutoScalingGroup(
            region, autoScalingGroupName, awsConfig, encryptedDataDetails, infrastructureMapping.getAppId());

        Map<String, com.amazonaws.services.ec2.model.Instance> latestEc2InstanceMap =
            latestEc2Instances.stream().collect(
                Collectors.toMap(com.amazonaws.services.ec2.model.Instance::getInstanceId, identity()));

        Collection<Instance> instancesInDB = asgInstanceMap.get(autoScalingGroupName);
        Map<String, Instance> instancesInDBMap = Maps.newHashMap();

        // If there are prior instances in db already
        if (isNotEmpty(instancesInDB)) {
          instancesInDB.forEach(instance -> {
            if (instance != null) {
              instancesInDBMap.put(getEc2InstanceId(instance), instance);
            }
          });
        }

        SetView<String> instancesToBeUpdated =
            Sets.intersection(latestEc2InstanceMap.keySet(), instancesInDBMap.keySet());

        // Find the instances that were yet to be added to db
        SetView<String> instancesToBeAdded = Sets.difference(latestEc2InstanceMap.keySet(), instancesInDBMap.keySet());

        if (asgNameDeploymentSummaryMap != null && !isAmi) {
          instancesToBeUpdated.forEach(ec2InstanceId -> {
            Instance oldInstance = instancesInDBMap.get(ec2InstanceId);
            com.amazonaws.services.ec2.model.Instance ec2Instance = latestEc2InstanceMap.get(ec2InstanceId);
            Instance instance = buildInstanceUsingEc2InstanceAndASG(null, ec2Instance, infrastructureMapping,
                autoScalingGroupName, asgNameDeploymentSummaryMap.get(autoScalingGroupName));
            if (oldInstance != null) {
              instanceService.update(instance, oldInstance.getUuid());
            } else {
              logger.error("Instance doesn't exist for given ec2 instance id {}", ec2InstanceId);
            }
          });

          logger.info("Instances to be updated {}", instancesToBeUpdated.size());
        }

        handleEc2InstanceDelete(instancesInDBMap, latestEc2InstanceMap);

        DeploymentSummary deploymentSummary;
        if (isNotEmpty(instancesToBeAdded)) {
          if (isAmi) {
            // newDeploymentInfo would be null in case of sync job.
            if ((asgNameDeploymentSummaryMap == null || !asgNameDeploymentSummaryMap.containsKey(autoScalingGroupName))
                && isNotEmpty(instancesInDB)) {
              Optional<Instance> instanceWithExecutionInfoOptional = getInstanceWithExecutionInfo(instancesInDB);
              if (!instanceWithExecutionInfoOptional.isPresent()) {
                logger.warn("Couldn't find an instance from a previous deployment for inframapping {}",
                    infrastructureMapping.getUuid());
                return;
              }

              DeploymentSummary deploymentSummaryFromPrevious =
                  DeploymentSummary.builder()
                      .deploymentInfo(AwsAutoScalingGroupDeploymentInfo.builder().build())
                      .build();
              generateDeploymentSummaryFromInstance(
                  instanceWithExecutionInfoOptional.get(), deploymentSummaryFromPrevious);
              deploymentSummary = deploymentSummaryFromPrevious;
            } else {
              deploymentSummary = getDeploymentSummaryForInstanceCreation(
                  asgNameDeploymentSummaryMap.get(autoScalingGroupName), rollback);
            }

            instancesToBeAdded.forEach(ec2InstanceId -> {
              com.amazonaws.services.ec2.model.Instance ec2Instance = latestEc2InstanceMap.get(ec2InstanceId);
              // change to asg based instance builder
              Instance instance = buildInstanceUsingEc2InstanceAndASG(
                  null, ec2Instance, infrastructureMapping, autoScalingGroupName, deploymentSummary);
              instanceService.save(instance);
            });
          } else {
            // If a trigger is configured on a new instance creation, it will go ahead and spin up a workflow
            triggerService.triggerExecutionByServiceInfra(
                infrastructureMapping.getAppId(), infrastructureMapping.getUuid());
          }

          logger.info("Instances to be added {}", instancesToBeAdded.size());
        }
      });
    }
  }

  private String getEc2InstanceId(Instance instance) {
    return ((AutoScalingGroupInstanceInfo) instance.getInstanceInfo()).getEc2Instance().getInstanceId();
  }

  protected Instance buildInstanceUsingEc2InstanceAndASG(String instanceId,
      com.amazonaws.services.ec2.model.Instance ec2Instance, InfrastructureMapping infraMapping,
      String autoScalingGroupName, DeploymentSummary newDeploymentSummary) {
    InstanceBuilder builder = buildInstanceBase(instanceId, infraMapping, newDeploymentSummary);
    setASGInstanceInfoAndKey(builder, ec2Instance, infraMapping.getUuid(), autoScalingGroupName);
    return builder.build();
  }

  private void setASGInstanceInfoAndKey(InstanceBuilder builder, com.amazonaws.services.ec2.model.Instance ec2Instance,
      String infraMappingId, String autoScalingGroupName) {
    String privateDnsName = buildHostInstanceKey(ec2Instance, infraMappingId, builder);
    InstanceInfo instanceInfo = AutoScalingGroupInstanceInfo.builder()
                                    .ec2Instance(ec2Instance)
                                    .hostName(privateDnsName)
                                    .autoScalingGroupName(autoScalingGroupName)
                                    .hostPublicDns(ec2Instance.getPublicDnsName())
                                    .build();

    builder.instanceInfo(instanceInfo);
  }

  private void handleEc2InstanceSyncWithAwsInfraMapping(Map<String, Instance> ec2InstanceIdInstanceMap,
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptedDataDetails, String region,
      AwsInfrastructureMapping awsInfrastructureMapping) {
    List<com.amazonaws.services.ec2.model.Instance> activeInstanceList =
        awsInfrastructureProvider.listFilteredInstances(awsInfrastructureMapping, awsConfig, encryptedDataDetails);

    deleteRunningEc2InstancesFromMap(ec2InstanceIdInstanceMap, activeInstanceList);
  }

  protected void handleEc2InstanceSync(Map<String, Instance> ec2InstanceIdInstanceMap, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptedDataDetails, String region) {
    // Check if the instances are still running. These instances were the ones that were stored with the old schema.
    if (ec2InstanceIdInstanceMap.size() > 0) {
      // we do not want to use any special filter here, if awsFilter is null then,
      // awsInfrastructureProvider.listFilteredInstances() uses default filter as "instance-state-name" = "running"
      AwsInfrastructureMapping awsInfrastructureMapping = AwsInfrastructureMapping.Builder.anAwsInfrastructureMapping()
                                                              .withRegion(region)
                                                              .withAwsInstanceFilter(null)
                                                              .withDeploymentType(DeploymentType.SSH.name())
                                                              .build();
      List<com.amazonaws.services.ec2.model.Instance> activeInstanceList =
          awsInfrastructureProvider.listFilteredInstances(awsInfrastructureMapping, awsConfig, encryptedDataDetails);

      deleteRunningEc2InstancesFromMap(ec2InstanceIdInstanceMap, activeInstanceList);
    }
  }

  private void deleteRunningEc2InstancesFromMap(Map<String, Instance> ec2InstanceIdInstanceMap,
      List<com.amazonaws.services.ec2.model.Instance> activeInstanceList) {
    Instance ec2instance = ec2InstanceIdInstanceMap.values().iterator().next();
    logger.info("Total no of Ec2 instances found in DB for AppId: {}: {}, No of Running instances found in aws:{}",
        ec2instance.getAppId(), ec2InstanceIdInstanceMap.size(), activeInstanceList.size());
    ec2InstanceIdInstanceMap.keySet().removeAll(
        activeInstanceList.stream().map(com.amazonaws.services.ec2.model.Instance::getInstanceId).collect(toSet()));

    Set<String> instanceIdsToBeDeleted = ec2InstanceIdInstanceMap.entrySet()
                                             .stream()
                                             .map(entry -> entry.getValue().getUuid())
                                             .collect(Collectors.toSet());

    if (isNotEmpty(instanceIdsToBeDeleted)) {
      logger.info("Instances to be deleted {}", instanceIdsToBeDeleted.size());
      instanceService.delete(instanceIdsToBeDeleted);
    }
  }

  protected List<com.amazonaws.services.ec2.model.Instance> getEc2InstancesFromAutoScalingGroup(String region,
      String autoScalingGroupName, AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String appId) {
    return awsAsgHelperServiceManager.listAutoScalingGroupInstances(
        awsConfig, encryptionDetails, region, autoScalingGroupName, appId);
  }
}
