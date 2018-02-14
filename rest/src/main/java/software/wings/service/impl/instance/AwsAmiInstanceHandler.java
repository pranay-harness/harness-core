package software.wings.service.impl.instance;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.inject.Singleton;

import software.wings.annotation.Encryptable;
import software.wings.api.AwsAutoScalingGroupDeploymentInfo;
import software.wings.api.DeploymentInfo;
import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.AwsConfig;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.exception.HarnessException;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.utils.Validator;

import java.util.List;
import java.util.Map;

/**
 * @author rktummala on 02/02/18
 */
@Singleton
public class AwsAmiInstanceHandler extends AwsInstanceHandler {
  @Override
  public void syncInstances(String appId, String infraMappingId) throws HarnessException {
    // Key - Auto scaling group with revision, Value - Instance
    Multimap<String, Instance> asgInstanceMap = ArrayListMultimap.create();
    syncInstancesInternal(appId, infraMappingId, asgInstanceMap, null);
  }

  private void syncInstancesInternal(String appId, String infraMappingId, Multimap<String, Instance> asgInstanceMap,
      DeploymentInfo newDeploymentInfo) throws HarnessException {
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

    handleAsgInstanceSync(
        region, asgInstanceMap, awsConfig, encryptedDataDetails, infrastructureMapping, newDeploymentInfo);
  }

  @Override
  public void handleNewDeployment(DeploymentInfo deploymentInfo) throws HarnessException {
    if (!(deploymentInfo instanceof AwsAutoScalingGroupDeploymentInfo)) {
      throw new HarnessException("Incompatible deployment type.");
    }

    AwsAutoScalingGroupDeploymentInfo asgDeploymentEvent = (AwsAutoScalingGroupDeploymentInfo) deploymentInfo;
    Multimap<String, Instance> asgInstanceMap = ArrayListMultimap.create();

    asgDeploymentEvent.getAutoScalingGroupNameList().stream().forEach(
        autoScalingGroupName -> asgInstanceMap.put(autoScalingGroupName, null));

    syncInstancesInternal(
        deploymentInfo.getAppId(), deploymentInfo.getInfraMappingId(), asgInstanceMap, deploymentInfo);
  }
}
