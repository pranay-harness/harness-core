package software.wings.cloudprovider.aws;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.CreateAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.CreateAutoScalingGroupResult;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.Instance;
import com.amazonaws.services.autoscaling.model.SetDesiredCapacityRequest;
import com.amazonaws.services.autoscaling.model.SetDesiredCapacityResult;
import com.amazonaws.services.ecs.AmazonECSClient;
import com.amazonaws.services.ecs.model.Cluster;
import com.amazonaws.services.ecs.model.CreateClusterRequest;
import com.amazonaws.services.ecs.model.CreateServiceRequest;
import com.amazonaws.services.ecs.model.CreateServiceResult;
import com.amazonaws.services.ecs.model.DeleteServiceRequest;
import com.amazonaws.services.ecs.model.DeleteServiceResult;
import com.amazonaws.services.ecs.model.DescribeClustersRequest;
import com.amazonaws.services.ecs.model.DescribeServicesRequest;
import com.amazonaws.services.ecs.model.Service;
import com.amazonaws.services.ecs.model.UpdateServiceRequest;
import com.amazonaws.services.ecs.model.UpdateServiceResult;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AwsConfig;
import software.wings.beans.ErrorCodes;
import software.wings.beans.SettingAttribute;
import software.wings.exception.WingsException;
import software.wings.service.impl.AwsHelperService;
import software.wings.utils.Misc;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Created by anubhaw on 12/28/16.
 */
@Singleton
public class EcsServiceImpl implements EcsService {
  @Inject private AwsHelperService awsHelperService;
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private ObjectMapper mapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

  @Override
  public void provisionNodes(
      SettingAttribute connectorConfig, String autoScalingGroupName, Integer desiredClusterSize) {
    AwsConfig awsConfig = validateAndGetAwsConfig(connectorConfig);
    AmazonAutoScalingClient amazonAutoScalingClient =
        awsHelperService.getAmazonAutoScalingClient(awsConfig.getAccessKey(), awsConfig.getSecretKey());

    // TODO: add validation for autoscalingGroupName and desiredClusterSize
    SetDesiredCapacityResult setDesiredCapacityResult =
        amazonAutoScalingClient.setDesiredCapacity(new SetDesiredCapacityRequest()
                                                       .withAutoScalingGroupName(autoScalingGroupName)
                                                       .withDesiredCapacity(desiredClusterSize));
  }

  @Override
  public void provisionNodes(
      SettingAttribute connectorConfig, Integer clusterSize, String launchConfigName, Map<String, Object> params) {
    AwsConfig awsConfig = validateAndGetAwsConfig(connectorConfig);

    AmazonECSClient amazonEcsClient =
        awsHelperService.getAmazonEcsClient(awsConfig.getAccessKey(), awsConfig.getSecretKey());
    amazonEcsClient.createCluster(new CreateClusterRequest().withClusterName((String) params.get("clusterName")));
    logger.info("Successfully created empty cluster " + params.get("clusterName"));
    Misc.quietSleep(2 * 1000); // TODO:: remove
    logger.info("Creating autoscaling group for cluster...");

    AmazonAutoScalingClient amazonAutoScalingClient =
        awsHelperService.getAmazonAutoScalingClient(awsConfig.getAccessKey(), awsConfig.getSecretKey());

    Integer maxSize = (Integer) params.computeIfAbsent("maxSize", s -> 2 * clusterSize); // default 200%
    Integer minSize = (Integer) params.computeIfAbsent("minSize", s -> clusterSize / 2); // default 50%
    String name = (String) params.get("autoScalingGroupName");
    String vpcZoneIdentifiers = (String) params.get("vpcZoneIdentifiers");
    List<String> availabilityZones = (List<String>) params.computeIfAbsent("availabilityZones", s -> null);

    CreateAutoScalingGroupResult createAutoScalingGroupResult =
        amazonAutoScalingClient.createAutoScalingGroup(new CreateAutoScalingGroupRequest()
                                                           .withLaunchConfigurationName(launchConfigName)
                                                           .withDesiredCapacity(clusterSize)
                                                           .withMaxSize(maxSize)
                                                           .withMinSize(minSize)
                                                           .withAutoScalingGroupName(name)
                                                           .withAvailabilityZones(availabilityZones)
                                                           .withVPCZoneIdentifier(vpcZoneIdentifiers));

    logger.info("Successfully created autoScalingGroup: {}", name);
    Misc.quietSleep(2 * 1000); // TODO:: remove

    while (!allInstanceInReadyState(amazonAutoScalingClient, name, clusterSize)) {
      Misc.quietSleep(5 * 1000);
    }

    while (!allInstancesRegisteredWithCluster(amazonEcsClient, (String) params.get("clusterName"), clusterSize)) {
      Misc.quietSleep(5 * 1000);
    }
    logger.info("All instaces are ready for deployment");
  }

  private boolean allInstancesRegisteredWithCluster(AmazonECSClient amazonEcsClient, String name, Integer clusterSize) {
    Cluster cluster =
        amazonEcsClient.describeClusters(new DescribeClustersRequest().withClusters(name)).getClusters().get(0);
    logger.info("Waiting for instances to register with cluster. {}/{} registered...",
        cluster.getRegisteredContainerInstancesCount(), clusterSize);

    return cluster.getRegisteredContainerInstancesCount() == clusterSize;
  }

  private boolean allInstanceInReadyState(
      AmazonAutoScalingClient amazonAutoScalingClient, String name, Integer clusterSize) {
    AutoScalingGroup autoScalingGroup =
        amazonAutoScalingClient
            .describeAutoScalingGroups(
                new DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames(Arrays.asList(name)))
            .getAutoScalingGroups()
            .get(0);
    List<Instance> instances = autoScalingGroup.getInstances();
    logger.info("Waiting for all instances to be ready. {}/{} ready...", instances.size(), clusterSize);
    return instances.size() != 0
        && instances.stream().allMatch(instance -> "InService".equals(instance.getLifecycleState()));
  }

  @Override
  public void deprovisionNodes(
      SettingAttribute connectorConfig, String autoScalingGroupName, Integer desiredClusterSize) {
    AwsConfig awsConfig = validateAndGetAwsConfig(connectorConfig);
    AmazonAutoScalingClient amazonAutoScalingClient =
        awsHelperService.getAmazonAutoScalingClient(awsConfig.getAccessKey(), awsConfig.getSecretKey());

    SetDesiredCapacityResult setDesiredCapacityResult =
        amazonAutoScalingClient.setDesiredCapacity(new SetDesiredCapacityRequest()
                                                       .withAutoScalingGroupName(autoScalingGroupName)
                                                       .withDesiredCapacity(desiredClusterSize));
  }

  @Override
  public String deployService(SettingAttribute connectorConfig, String serviceDefinition) {
    AwsConfig awsConfig = validateAndGetAwsConfig(connectorConfig);
    AmazonECSClient amazonECSClient =
        awsHelperService.getAmazonEcsClient(awsConfig.getAccessKey(), awsConfig.getSecretKey());
    CreateServiceRequest createServiceRequest = null;
    try {
      createServiceRequest = mapper.readValue(serviceDefinition, CreateServiceRequest.class);
    } catch (IOException ex) {
      ex.printStackTrace();
    }
    logger.info("Begin service deployment " + createServiceRequest.getServiceName());
    CreateServiceResult createServiceResult = amazonECSClient.createService(createServiceRequest);

    while (!allDesiredTaskRuning(amazonECSClient, createServiceRequest)) {
      Misc.quietSleep(5 * 1000);
    }

    return createServiceResult.getService().getServiceArn();
  }

  private boolean allDesiredTaskRuning(AmazonECSClient amazonECSClient, CreateServiceRequest createServiceRequest) {
    Service service = amazonECSClient
                          .describeServices(new DescribeServicesRequest()
                                                .withCluster(createServiceRequest.getCluster())
                                                .withServices(createServiceRequest.getServiceName()))
                          .getServices()
                          .get(0);

    logger.info("Waiting for for pending tasks to finish. {}/{} running ...", service.getRunningCount(),
        service.getDesiredCount());
    return service.getDesiredCount() == service.getRunningCount();
  }

  @Override
  public void rollingUpdateService(SettingAttribute connectorConfig, String serviceDefinition) {
    AwsConfig awsConfig = validateAndGetAwsConfig(connectorConfig);
    AmazonECSClient amazonECSClient =
        awsHelperService.getAmazonEcsClient(awsConfig.getAccessKey(), awsConfig.getSecretKey());
    UpdateServiceRequest updateServiceRequest = null;
    try {
      updateServiceRequest = mapper.readValue(serviceDefinition, UpdateServiceRequest.class);
    } catch (IOException ex) {
      ex.printStackTrace();
    }
    UpdateServiceResult updateServiceResult = amazonECSClient.updateService(updateServiceRequest);
  }

  @Override
  public void deleteService(SettingAttribute connectorConfig, String serviceName) {
    AwsConfig awsConfig = validateAndGetAwsConfig(connectorConfig);
    AmazonECSClient amazonECSClient =
        awsHelperService.getAmazonEcsClient(awsConfig.getAccessKey(), awsConfig.getSecretKey());
    DeleteServiceResult deleteServiceResult =
        amazonECSClient.deleteService(new DeleteServiceRequest().withService(serviceName));
  }

  @Override
  public void provisionTasks(
      SettingAttribute connectorConfig, String clusterName, String serviceName, Integer desiredCount) {
    AwsConfig awsConfig = validateAndGetAwsConfig(connectorConfig);
    AmazonECSClient amazonECSClient =
        awsHelperService.getAmazonEcsClient(awsConfig.getAccessKey(), awsConfig.getSecretKey());
    UpdateServiceRequest updateServiceRequest =
        new UpdateServiceRequest().withCluster(clusterName).withService(serviceName).withDesiredCount(desiredCount);
    amazonECSClient.updateService(updateServiceRequest);
  }

  @Override
  public void deprovisionTasks(SettingAttribute connectorConfig, String serviceName, Integer desiredCount) {
    AwsConfig awsConfig = validateAndGetAwsConfig(connectorConfig);
    AmazonECSClient amazonECSClient =
        awsHelperService.getAmazonEcsClient(awsConfig.getAccessKey(), awsConfig.getSecretKey());
    UpdateServiceRequest updateServiceRequest =
        new UpdateServiceRequest().withService(serviceName).withDesiredCount(desiredCount);
    amazonECSClient.updateService(updateServiceRequest);
  }

  private AwsConfig validateAndGetAwsConfig(SettingAttribute connectorConfig) {
    if (connectorConfig == null || connectorConfig.getValue() == null
        || !(connectorConfig.getValue() instanceof AwsConfig)) {
      throw new WingsException(ErrorCodes.INVALID_REQUEST, "message", "connectorConfig is not of type AwsConfig");
    }
    return (AwsConfig) connectorConfig.getValue();
  }
}
