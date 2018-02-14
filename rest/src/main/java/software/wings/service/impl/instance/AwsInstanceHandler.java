package software.wings.service.impl.instance;

import static java.util.stream.Collectors.toSet;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.commons.collections.CollectionUtils;
import software.wings.annotation.Encryptable;
import software.wings.api.AwsAutoScalingGroupDeploymentInfo;
import software.wings.api.DeploymentInfo;
import software.wings.beans.AwsConfig;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.Instance.InstanceBuilder;
import software.wings.beans.infrastructure.instance.info.AutoScalingGroupInstanceInfo;
import software.wings.beans.infrastructure.instance.info.Ec2InstanceInfo;
import software.wings.beans.infrastructure.instance.info.InstanceInfo;
import software.wings.beans.infrastructure.instance.key.HostInstanceKey;
import software.wings.exception.HarnessException;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.impl.AwsInfrastructureProvider;
import software.wings.utils.Validator;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author rktummala on 02/01/18
 */
@Singleton
public class AwsInstanceHandler extends InstanceHandler {
  @Inject protected AwsHelperService awsHelperService;
  @Inject protected AwsInfrastructureProvider awsInfrastructureProvider;

  @Override
  public void syncInstances(String appId, String infraMappingId) throws HarnessException {
    // Key - Auto scaling group with revision, Value - Instance
    Multimap<String, Instance> asgInstanceMap = ArrayListMultimap.create();

    InfrastructureMapping infrastructureMapping = infraMappingService.get(appId, infraMappingId);
    Validator.notNullCheck("Infra mapping is null for id:" + infraMappingId, infrastructureMapping);

    if (!(infrastructureMapping instanceof AwsInfrastructureMapping)) {
      String msg =
          "Incompatible infra mapping type. Expecting aws type. Found:" + infrastructureMapping.getInfraMappingType();
      logger.error(msg);
      throw new HarnessException(msg);
    }

    AwsInfrastructureMapping awsInfraMapping = (AwsInfrastructureMapping) infrastructureMapping;

    // key - ec2 instance id, value - instance
    Map<String, Instance> ec2InstanceIdInstanceMap = Maps.newHashMap();

    loadInstanceMapBasedOnType(appId, infraMappingId, asgInstanceMap, ec2InstanceIdInstanceMap);

    SettingAttribute cloudProviderSetting = settingsService.get(infrastructureMapping.getComputeProviderSettingId());
    AwsConfig awsConfig = (AwsConfig) cloudProviderSetting.getValue();
    List<EncryptedDataDetail> encryptedDataDetails =
        secretManager.getEncryptionDetails((Encryptable) cloudProviderSetting.getValue(), null, null);

    String region = awsInfraMapping.getRegion();

    // Check if the instances are still running. These instances were either the ones that were stored with the old
    // schema or the instances created using aws infra mapping with filter.
    if (ec2InstanceIdInstanceMap.size() > 0) {
      if (awsInfraMapping.getAwsInstanceFilter() != null) {
        List<com.amazonaws.services.ec2.model.Instance> filteredInstanceList =
            awsInfrastructureProvider.listFilteredInstances(awsInfraMapping, awsConfig, encryptedDataDetails);
        ec2InstanceIdInstanceMap.keySet().removeAll(filteredInstanceList);
      } else {
        handleEc2InstanceSync(ec2InstanceIdInstanceMap, awsConfig, encryptedDataDetails, region);
      }
    }

    handleAsgInstanceSync(region, asgInstanceMap, awsConfig, encryptedDataDetails, infrastructureMapping, null);
  }

  @Override
  public void handleNewDeployment(DeploymentInfo deploymentInfo) throws HarnessException {
    // All the new deployments are either handled at ASGInstanceHandler(for Aws ssh with asg) or InstanceHelper (for Aws
    // ssh with or without filter)
    throw new HarnessException("Deployments should be handled at InstanceHelper for aws ssh type except for with ASG.");
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
      DeploymentInfo deploymentInfo) {
    Map<String, DeploymentInfo> asgDeploymentInfoMap = Maps.newHashMap();
    if (deploymentInfo != null) {
      AwsAutoScalingGroupDeploymentInfo asgDeploymentInfo = (AwsAutoScalingGroupDeploymentInfo) deploymentInfo;
      asgDeploymentInfo.getAutoScalingGroupNameList().stream().forEach(
          autoScalingGroupName -> asgDeploymentInfoMap.put(autoScalingGroupName, asgDeploymentInfo));
    }

    // This is to handle the case of the instances stored in the new schema.
    if (asgInstanceMap.size() > 0) {
      asgInstanceMap.keySet().stream().forEach(autoScalingGroupName -> {
        List<com.amazonaws.services.ec2.model.Instance> latestEc2Instances =
            getEc2InstancesFromAutoScalingGroup(region, autoScalingGroupName, awsConfig, encryptedDataDetails);

        Map<String, com.amazonaws.services.ec2.model.Instance> latestEc2InstanceMap =
            latestEc2Instances.stream().collect(Collectors.toMap(ec2Instance -> {
              String privateDnsName = ec2Instance.getPrivateDnsName();
              if (privateDnsName != null) {
                return awsHelperService.getHostnameFromPrivateDnsName(privateDnsName);
              }
              return null;
            }, ec2Instance -> ec2Instance));

        Collection<Instance> instancesInDB = asgInstanceMap.get(autoScalingGroupName);
        Map<String, Instance> instancesInDBMap = Maps.newHashMap();

        // If there are prior instances in db already
        if (CollectionUtils.isNotEmpty(instancesInDB)) {
          instancesInDB.stream().forEach(instance -> {
            if (instance != null) {
              instancesInDBMap.put(instance.getHostInstanceKey().getHostName(), instance);
            }
          });
        }

        SetView<String> instancesToBeUpdated =
            Sets.intersection(latestEc2InstanceMap.keySet(), instancesInDBMap.keySet());

        // Find the instances that were yet to be added to db
        SetView<String> instancesToBeAdded = Sets.difference(latestEc2InstanceMap.keySet(), instancesInDBMap.keySet());

        instancesToBeUpdated.stream().forEach(ec2InstanceId -> {
          Instance instance = instancesInDBMap.get(ec2InstanceId);
          String uuid = null;
          if (instance != null) {
            uuid = instance.getUuid();
          }
          com.amazonaws.services.ec2.model.Instance ec2Instance = latestEc2InstanceMap.get(ec2InstanceId);
          instance = buildInstanceUsingEc2InstanceAndASG(uuid, ec2Instance, infrastructureMapping, autoScalingGroupName,
              asgDeploymentInfoMap.get(autoScalingGroupName));
          instanceService.saveOrUpdate(instance);
        });

        handleEc2InstanceDelete(instancesInDBMap, latestEc2InstanceMap);

        if (instancesToBeAdded.size() > 0) {
          instancesToBeAdded.stream().forEach(ec2InstanceId -> {
            com.amazonaws.services.ec2.model.Instance ec2Instance = latestEc2InstanceMap.get(ec2InstanceId);
            // change to asg based instance builder
            Instance instance = buildInstanceUsingEc2InstanceAndASG(null, ec2Instance, infrastructureMapping,
                autoScalingGroupName, asgDeploymentInfoMap.get(autoScalingGroupName));
            instanceService.save(instance);
          });

          // Call this only in periodic sync case
          if (deploymentInfo == null) {
            // If a trigger is configured on a new instance creation, it will go ahead and spin up a workflow
            triggerService.triggerExecutionByServiceInfra(
                infrastructureMapping.getAppId(), infrastructureMapping.getUuid());
          }
        }
      });
    }
  }

  protected Instance buildInstanceUsingEc2InstanceAndASG(String instanceId,
      com.amazonaws.services.ec2.model.Instance ec2Instance, InfrastructureMapping infraMapping,
      String autoScalingGroupName, DeploymentInfo newDeploymentInfo) {
    InstanceBuilder builder = instanceHelper.buildInstanceBase(instanceId, infraMapping, newDeploymentInfo);
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

  /**
   *
   * @param ec2Instance Ec2 instance
   * @param infraMappingId  Infra mapping id
   * @param instanceBuilder Instance builder
   * @return privateDnsName private dns name
   */
  protected String buildHostInstanceKey(
      com.amazonaws.services.ec2.model.Instance ec2Instance, String infraMappingId, InstanceBuilder instanceBuilder) {
    String privateDnsNameWithSuffix = ec2Instance.getPrivateDnsName();
    String privateDnsName = privateDnsNameWithSuffix.substring(0, privateDnsNameWithSuffix.indexOf('.'));
    HostInstanceKey hostInstanceKey =
        HostInstanceKey.builder().hostName(privateDnsName).infraMappingId(infraMappingId).build();
    instanceBuilder.hostInstanceKey(hostInstanceKey);
    return privateDnsName;
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
                                                              .build();
      List<com.amazonaws.services.ec2.model.Instance> activeInstanceList =
          awsInfrastructureProvider.listFilteredInstances(awsInfrastructureMapping, awsConfig, encryptedDataDetails);

      ec2InstanceIdInstanceMap.keySet().removeAll(
          activeInstanceList.stream().map(instance -> instance.getInstanceId()).collect(toSet()));

      Set<String> instanceIdsToBeDeleted = ec2InstanceIdInstanceMap.entrySet()
                                               .stream()
                                               .map(entry -> entry.getValue().getUuid())
                                               .collect(Collectors.toSet());

      if (CollectionUtils.isNotEmpty(instanceIdsToBeDeleted)) {
        instanceService.delete(instanceIdsToBeDeleted);
      }
    }
  }

  protected void handleEc2InstanceDelete(Map<String, Instance> instancesInDBMap,
      Map<String, com.amazonaws.services.ec2.model.Instance> latestEc2InstanceMap) {
    // Find the instances that are no longer present and to be deleted from db.
    SetView<String> instancesToBeDeleted = Sets.difference(instancesInDBMap.keySet(), latestEc2InstanceMap.keySet());

    Set<String> instanceIdsToBeDeleted = new HashSet<>();
    instancesToBeDeleted.stream().forEach(ec2InstanceId -> {
      Instance instance = instancesInDBMap.get(ec2InstanceId);
      if (instance != null) {
        instanceIdsToBeDeleted.add(instance.getUuid());
      }
    });

    if (CollectionUtils.isNotEmpty(instanceIdsToBeDeleted)) {
      instanceService.delete(instanceIdsToBeDeleted);
    }
  }

  protected List<com.amazonaws.services.ec2.model.Instance> getEc2InstancesFromAutoScalingGroup(
      String region, String autoScalingGroupName, AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails) {
    return awsHelperService.listAutoScalingGroupInstances(awsConfig, encryptionDetails, region, autoScalingGroupName);
  }
}
