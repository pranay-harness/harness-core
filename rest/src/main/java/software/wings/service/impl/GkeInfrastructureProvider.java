package software.wings.service.impl;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.GkeConfig;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.beans.infrastructure.Host;
import software.wings.cloudprovider.gke.GkeClusterService;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.exception.WingsException;
import software.wings.service.intfc.InfrastructureProvider;
import software.wings.utils.Misc;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

import static software.wings.beans.ErrorCode.INIT_TIMEOUT;
import static software.wings.beans.ErrorCode.INVALID_ARGUMENT;

/**
 * Created by bzane on 2/27/17
 */
@Singleton
public class GkeInfrastructureProvider implements InfrastructureProvider {
  private static final int SLEEP_INTERVAL = 30 * 1000;
  private static final int RETRY_COUNTER = (10 * 60 * 1000) / SLEEP_INTERVAL; // 10 minutes

  private final Logger logger = LoggerFactory.getLogger(GkeInfrastructureProvider.class);

  @Inject private GkeHelperService gkeHelperService;
  @Inject private GkeClusterService gkeClusterService;

  @Override
  public PageResponse<Host> listHosts(SettingAttribute computeProviderSetting, PageRequest<Host> req) {
    GkeConfig gkeConfig = validateAndGetGkeConfig(computeProviderSetting);

    //    DescribeInstancesResult describeInstancesResult =
    //        amazonEC2Client.describeInstances(new DescribeInstancesRequest().withFilters(new
    //        Filter("instance-state-name", Arrays.asList("running"))));
    //
    //    List<Host> awsHosts = describeInstancesResult.getReservations().stream().flatMap(reservation ->
    //    reservation.getInstances().stream())
    //        .map(instance ->
    //        anAwsHost().withAppId(Base.GLOBAL_APP_ID).withHostName(instance.getPublicDnsName()).withInstance(instance).build())
    //        .collect(Collectors.toList());
    return PageResponse.Builder.aPageResponse().withResponse(null).build();
  }

  @Override
  public void deleteHost(String appId, String infraMappingId, String hostName) {
    //    hostService.deleteByHostName(appId, infraMappingId, hostName);
  }

  @Override
  public void updateHostConnAttrs(InfrastructureMapping infrastructureMapping, String hostConnectionAttrs) {
    //    hostService.updateHostConnectionAttrByInfraMappingId(infrastructureMapping.getAppId(),
    //    infrastructureMapping.getUuid(), hostConnectionAttrs);
  }

  @Override
  public void deleteHostByInfraMappingId(String appId, String infraMappingId) {
    //    hostService.deleteByInfraMappingId(appId, infraMappingId);
  }

  private GkeConfig validateAndGetGkeConfig(SettingAttribute computeProviderSetting) {
    if (computeProviderSetting == null || !(computeProviderSetting.getValue() instanceof GkeConfig)) {
      throw new WingsException(INVALID_ARGUMENT, "message", "InvalidConfiguration");
    }

    return (GkeConfig) computeProviderSetting.getValue();
  }

  @Override
  public Host saveHost(Host host) {
    return null;
  }

  public List<Host> provisionHosts(
      SettingAttribute computeProviderSetting, String launcherConfigName, int instanceCount) {
    GkeConfig gkeConfig = validateAndGetGkeConfig(computeProviderSetting);

    //    AmazonAutoScalingClient amazonAutoScalingClient =
    //    gkeHelperService.getAmazonAutoScalingClient(gkeConfig.getAccessKey(), gkeConfig.getSecretKey());
    //    AmazonEC2Client amazonEc2Client = gkeHelperService.getAmazonEc2Client(gkeConfig.getAccessKey(),
    //    gkeConfig.getSecretKey());
    //
    //    List<LaunchConfiguration> launchConfigurations =
    //        amazonAutoScalingClient.describeLaunchConfigurations(new
    //        DescribeLaunchConfigurationsRequest().withLaunchConfigurationNames(launcherConfigName))
    //            .getLaunchConfigurations();
    //    LaunchConfiguration launchConfiguration = launchConfigurations.get(0);
    //
    //    RunInstancesRequest runInstancesRequest =
    //        new
    //        RunInstancesRequest().withImageId(launchConfiguration.getImageId()).withInstanceType(launchConfiguration.getInstanceType())
    //            .withMinCount(instanceCount).withMaxCount(instanceCount).withKeyName(launchConfiguration.getKeyName())
    //            .withIamInstanceProfile(new
    //            IamInstanceProfileSpecification().withName(launchConfiguration.getIamInstanceProfile()))
    //            .withSecurityGroupIds(launchConfiguration.getSecurityGroups()).withUserData(launchConfiguration.getUserData());
    //
    //    List<Instance> instances = amazonEc2Client.runInstances(runInstancesRequest).getReservation().getInstances();
    //    List<String> instancesIds = instances.stream().map(Instance::getInstanceId).collect(Collectors.toList());
    //    logger.info("Provisioned hosts count = {} and provisioned hosts instance ids = {}", instancesIds.size(),
    //    instancesIds);
    //
    //    waitForAllInstancesToBeReady(amazonEc2Client, instancesIds);
    //    logger.info("All instances are in running state");
    //
    //    return amazonEc2Client.describeInstances(new
    //    DescribeInstancesRequest().withInstanceIds(instancesIds)).getReservations().stream()
    //        .flatMap(reservation -> reservation.getInstances().stream())
    //        .map(instance ->
    //        anAwsHost().withHostName(instance.getPublicDnsName()).withInstance(instance).build()).collect(Collectors.toList());
    return null;
  }

  public void deProvisionHosts(
      String appId, String infraMappingId, SettingAttribute computeProviderSetting, List<String> hostNames) {
    GkeConfig gkeConfig = validateAndGetGkeConfig(computeProviderSetting);
    //    AmazonEC2Client amazonEc2Client = gkeHelperService.getAmazonEc2Client(gkeConfig.getAccessKey(),
    //    gkeConfig.getSecretKey());
    //
    //    List<AwsHost> awsHosts = hostService.list(aPageRequest().addFilter("appId", Operator.EQ,
    //    appId).addFilter("infraMappingId", Operator.EQ, infraMappingId)
    //        .addFilter("hostNames", Operator.IN, hostNames).build()).getResponse();
    //
    //    List<String> hostInstanceId = awsHosts.stream().map(host -> ((AwsHost)
    //    host).getInstance().getInstanceId()).collect(Collectors.toList()); TerminateInstancesResult
    //    terminateInstancesResult = amazonEc2Client.terminateInstances(new TerminateInstancesRequest(hostInstanceId));
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

  public List<String> listClusterNames(SettingAttribute computeProviderSetting) {
    GkeConfig gkeConfig = validateAndGetGkeConfig(computeProviderSetting);
    return gkeClusterService.listClusters(ImmutableMap.of(
        "projectId", gkeConfig.getProjectId(), "appName", gkeConfig.getAppName(), "zone", gkeConfig.getZone()));
  }
}
