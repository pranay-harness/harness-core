package software.wings.service.impl;

import static software.wings.beans.ErrorCodes.INIT_TIMEOUT;
import static software.wings.beans.ErrorCodes.INVALID_ARGUMENT;
import static software.wings.beans.infrastructure.AwsHost.Builder.anAwsHost;
import static software.wings.dl.PageRequest.Builder.aPageRequest;

import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.DescribeLaunchConfigurationsRequest;
import com.amazonaws.services.autoscaling.model.LaunchConfiguration;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.IamInstanceProfileSpecification;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesResult;
import com.amazonaws.services.ecs.AmazonECSClient;
import com.amazonaws.services.ecs.model.ListClustersRequest;
import com.amazonaws.services.ecs.model.ListClustersResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AwsConfig;
import software.wings.beans.Base;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.SettingAttribute;
import software.wings.beans.infrastructure.AwsHost;
import software.wings.beans.infrastructure.Host;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.exception.WingsException;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.InfrastructureProvider;
import software.wings.utils.Misc;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by anubhaw on 10/4/16.
 */
@Singleton
public class AwsInfrastructureProvider implements InfrastructureProvider {
  @Inject private AwsHelperService awsHelperService;
  @Inject private HostService hostService;
  private final Logger logger = LoggerFactory.getLogger(AwsInfrastructureProvider.class);

  private static final int SLEEP_INTERVAL = 5 * 1000;
  private static final int RETRY_COUNTER = (10 * 60 * 1000) / SLEEP_INTERVAL; // 10 minutes

  @Override
  public PageResponse<Host> listHosts(SettingAttribute computeProviderSetting, PageRequest<Host> req) {
    AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);

    AmazonEC2Client amazonEC2Client =
        awsHelperService.getAmazonEc2Client(awsConfig.getAccessKey(), awsConfig.getSecretKey());
    DescribeInstancesResult describeInstancesResult = amazonEC2Client.describeInstances(
        new DescribeInstancesRequest().withFilters(new Filter("instance-state-name", Arrays.asList("running"))));

    List<Host> awsHosts = describeInstancesResult.getReservations()
                              .stream()
                              .flatMap(reservation -> reservation.getInstances().stream())
                              .map(instance
                                  -> anAwsHost()
                                         .withAppId(Base.GLOBAL_APP_ID)
                                         .withHostName(instance.getPublicDnsName())
                                         .withInstance(instance)
                                         .build())
                              .collect(Collectors.toList());
    return PageResponse.Builder.aPageResponse().withResponse(awsHosts).build();
  }

  @Override
  public void deleteHost(String appId, String infraMappingId, String hostName) {
    hostService.deleteByHostName(appId, infraMappingId, hostName);
  }

  @Override
  public void updateHostConnAttrs(InfrastructureMapping infrastructureMapping, String hostConnectionAttrs) {
    hostService.updateHostConnectionAttrByInfraMappingId(
        infrastructureMapping.getAppId(), infrastructureMapping.getUuid(), hostConnectionAttrs);
  }

  @Override
  public void deleteHostByInfraMappingId(String appId, String infraMappingId) {
    hostService.deleteByInfraMappingId(appId, infraMappingId);
  }

  private AwsConfig validateAndGetAwsConfig(SettingAttribute computeProviderSetting) {
    if (computeProviderSetting == null || !(computeProviderSetting.getValue() instanceof AwsConfig)) {
      throw new WingsException(INVALID_ARGUMENT, "message", "InvalidConfiguration");
    }

    return (AwsConfig) computeProviderSetting.getValue();
  }

  @Override
  public Host saveHost(Host host) {
    return hostService.saveHost(host);
  }

  public List<Host> provisionHosts(
      SettingAttribute computeProviderSetting, String launcherConfigName, int instanceCount) {
    AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);

    AmazonAutoScalingClient amazonAutoScalingClient =
        awsHelperService.getAmazonAutoScalingClient(awsConfig.getAccessKey(), awsConfig.getSecretKey());
    AmazonEC2Client amazonEc2Client =
        awsHelperService.getAmazonEc2Client(awsConfig.getAccessKey(), awsConfig.getSecretKey());

    List<LaunchConfiguration> launchConfigurations =
        amazonAutoScalingClient
            .describeLaunchConfigurations(
                new DescribeLaunchConfigurationsRequest().withLaunchConfigurationNames(launcherConfigName))
            .getLaunchConfigurations();
    LaunchConfiguration launchConfiguration = launchConfigurations.get(0);

    RunInstancesRequest runInstancesRequest =
        new RunInstancesRequest()
            .withImageId(launchConfiguration.getImageId())
            .withInstanceType(launchConfiguration.getInstanceType())
            .withMinCount(instanceCount)
            .withMaxCount(instanceCount)
            .withKeyName(launchConfiguration.getKeyName())
            .withIamInstanceProfile(
                new IamInstanceProfileSpecification().withName(launchConfiguration.getIamInstanceProfile()))
            .withSecurityGroupIds(launchConfiguration.getSecurityGroups())
            .withUserData(launchConfiguration.getUserData());

    List<Instance> instances = amazonEc2Client.runInstances(runInstancesRequest).getReservation().getInstances();
    List<String> instancesIds = instances.stream().map(Instance::getInstanceId).collect(Collectors.toList());
    logger.info(
        "Provisioned hosts count = {} and provisioned hosts instance ids = {}", instancesIds.size(), instancesIds);

    waitForAllInstancesToBeReady(amazonEc2Client, instancesIds);
    logger.info("All instances are in running state");

    return amazonEc2Client.describeInstances(new DescribeInstancesRequest().withInstanceIds(instancesIds))
        .getReservations()
        .stream()
        .flatMap(reservation -> reservation.getInstances().stream())
        .map(instance -> anAwsHost().withHostName(instance.getPrivateDnsName()).withInstance(instance).build())
        .collect(Collectors.toList());
  }

  public void deProvisionHosts(
      String appId, String infraMappingId, SettingAttribute computeProviderSetting, List<String> hostNames) {
    AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);
    AmazonEC2Client amazonEc2Client =
        awsHelperService.getAmazonEc2Client(awsConfig.getAccessKey(), awsConfig.getSecretKey());

    List<AwsHost> awsHosts = hostService
                                 .list(aPageRequest()
                                           .addFilter("appId", Operator.EQ, appId)
                                           .addFilter("infraMappingId", Operator.EQ, infraMappingId)
                                           .addFilter("hostNames", Operator.IN, hostNames)
                                           .build())
                                 .getResponse();

    List<String> hostInstanceId =
        awsHosts.stream().map(host -> ((AwsHost) host).getInstance().getInstanceId()).collect(Collectors.toList());
    TerminateInstancesResult terminateInstancesResult =
        amazonEc2Client.terminateInstances(new TerminateInstancesRequest(hostInstanceId));
  }

  private void waitForAllInstancesToBeReady(AmazonEC2Client amazonEC2Client, List<String> instancesIds) {
    int retryCount = RETRY_COUNTER;
    while (!allInstanceInReadyState(amazonEC2Client, instancesIds)) {
      if (retryCount-- <= 0) {
        throw new WingsException(INIT_TIMEOUT, "message", "Not all instances in running state");
      }
      logger.info("Waiting for all instances to be in running state");
      Misc.quietSleep(SLEEP_INTERVAL);
    }
  }

  private boolean allInstanceInReadyState(AmazonEC2Client amazonEC2Client, List<String> instanceIds) {
    DescribeInstancesResult describeInstancesResult =
        amazonEC2Client.describeInstances(new DescribeInstancesRequest().withInstanceIds(instanceIds));
    boolean allRunning = describeInstancesResult.getReservations()
                             .stream()
                             .flatMap(reservation -> reservation.getInstances().stream())
                             .allMatch(instance -> instance.getState().getName().equals("running"));
    return allRunning;
  }

  public List<LaunchConfiguration> listLaunchConfigurations(SettingAttribute computeProviderSetting) {
    AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);
    AmazonAutoScalingClient amazonAutoScalingClient =
        awsHelperService.getAmazonAutoScalingClient(awsConfig.getAccessKey(), awsConfig.getSecretKey());
    // TODO:: remove direct usage of LaunchConfiguration
    return amazonAutoScalingClient.describeLaunchConfigurations().getLaunchConfigurations();
  }

  public List<String> listClusterNames(SettingAttribute computeProviderSetting) {
    AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);
    AmazonECSClient amazonEcsClient =
        awsHelperService.getAmazonEcsClient(awsConfig.getAccessKey(), awsConfig.getSecretKey());
    ListClustersResult listClustersResult = amazonEcsClient.listClusters(new ListClustersRequest());
    return listClustersResult.getClusterArns()
        .stream()
        .map(awsHelperService::getIdFromArn)
        .collect(Collectors.toList());
  }
}
