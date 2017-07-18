package software.wings.service.impl;

import static java.util.stream.Collectors.toList;
import static software.wings.beans.ErrorCode.INIT_TIMEOUT;
import static software.wings.beans.ErrorCode.INVALID_ARGUMENT;
import static software.wings.beans.infrastructure.AwsHost.Builder.anAwsHost;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.dl.PageResponse.Builder.aPageResponse;

import com.google.common.collect.Lists;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.DescribeLaunchConfigurationsRequest;
import com.amazonaws.services.autoscaling.model.LaunchConfiguration;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeVpcsRequest;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.IamInstanceProfileSpecification;
import com.amazonaws.services.ec2.model.Image;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Region;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesResult;
import com.amazonaws.services.ec2.model.Vpc;
import com.amazonaws.services.ecs.AmazonECSClient;
import com.amazonaws.services.ecs.model.ListClustersRequest;
import com.amazonaws.services.ecs.model.ListClustersResult;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeLoadBalancersRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.LoadBalancer;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetGroup;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.identitymanagement.model.InstanceProfile;
import com.amazonaws.services.identitymanagement.model.ListRolesRequest;
import com.amazonaws.services.identitymanagement.model.Role;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.app.MainConfiguration;
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
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by anubhaw on 10/4/16.
 */
@Singleton
public class AwsInfrastructureProvider implements InfrastructureProvider {
  private static final int SLEEP_INTERVAL = 30 * 1000;
  private static final int RETRY_COUNTER = (10 * 60 * 1000) / SLEEP_INTERVAL; // 10 minutes
  private final Logger logger = LoggerFactory.getLogger(AwsInfrastructureProvider.class);
  @Inject private AwsHelperService awsHelperService;
  @Inject private HostService hostService;
  @Inject private MainConfiguration mainConfiguration;

  @Override
  public PageResponse<Host> listHosts(String region, SettingAttribute computeProviderSetting, PageRequest<Host> req) {
    AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);

    AmazonEC2Client amazonEC2Client =
        awsHelperService.getAmazonEc2Client(region, awsConfig.getAccessKey(), awsConfig.getSecretKey());
    DescribeInstancesResult describeInstancesResult = amazonEC2Client.describeInstances(
        new DescribeInstancesRequest().withFilters(new Filter("instance-state-name", Arrays.asList("running"))));

    List<Host> awsHosts =
        describeInstancesResult.getReservations()
            .stream()
            .flatMap(reservation -> reservation.getInstances().stream())
            .map(instance
                -> anAwsHost()
                       .withAppId(Base.GLOBAL_APP_ID)
                       .withHostName(awsHelperService.getHostnameFromDnsName(instance.getPrivateDnsName()))
                       .withPublicDns(instance.getPublicDnsName())
                       .withInstance(instance)
                       .build())
            .collect(toList());
    return aPageResponse().withResponse(awsHosts).build();
  }

  @Override
  public void deleteHost(String appId, String infraMappingId, String publicDns) {
    hostService.deleteByPublicDns(appId, infraMappingId, publicDns);
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
      String region, SettingAttribute computeProviderSetting, String launcherConfigName, int instanceCount) {
    AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);

    AmazonAutoScalingClient amazonAutoScalingClient = awsHelperService.getAmazonAutoScalingClient(
        Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey());
    AmazonEC2Client amazonEc2Client =
        awsHelperService.getAmazonEc2Client(region, awsConfig.getAccessKey(), awsConfig.getSecretKey());

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
    List<String> instancesIds = instances.stream().map(Instance::getInstanceId).collect(toList());
    logger.info(
        "Provisioned hosts count = {} and provisioned hosts instance ids = {}", instancesIds.size(), instancesIds);

    waitForAllInstancesToBeReady(amazonEc2Client, instancesIds);
    logger.info("All instances are in running state");

    List<Instance> readyInstances =
        amazonEc2Client.describeInstances(new DescribeInstancesRequest().withInstanceIds(instancesIds))
            .getReservations()
            .stream()
            .flatMap(reservation -> reservation.getInstances().stream())
            .collect(Collectors.toList());

    for (Instance instance : readyInstances) {
      int retryCount = RETRY_COUNTER;
      String hostname = awsHelperService.getHostnameFromDnsName(instance.getPublicDnsName());
      while (!awsHelperService.canConnectToHost(hostname, 22, SLEEP_INTERVAL)) {
        if (retryCount-- <= 0) {
          logger.error("Could not verify connection to newly provisioned instances [{}] ", instancesIds);
          try {
            amazonEc2Client.terminateInstances(new TerminateInstancesRequest(instancesIds));
            logger.error("Terminated provisioned instances [{}] ", instancesIds);
          } catch (Exception ignoredException) {
            ignoredException.printStackTrace();
          }
          throw new WingsException(INIT_TIMEOUT, "message", "Couldn't connect to provisioned host");
        }
        Misc.sleepWithRuntimeException(SLEEP_INTERVAL);
        logger.info("Couldn't connect to host {}. {} retry attempts left ", hostname, retryCount);
      }
      logger.info("Successfully connected to host {} in {} retry attempts", hostname, RETRY_COUNTER - retryCount);
    }

    return amazonEc2Client.describeInstances(new DescribeInstancesRequest().withInstanceIds(instancesIds))
        .getReservations()
        .stream()
        .flatMap(reservation -> reservation.getInstances().stream())
        .map(instance
            -> anAwsHost()
                   .withHostName(awsHelperService.getHostnameFromDnsName(instance.getPublicDnsName()))
                   .withInstance(instance)
                   .build())
        .collect(toList());
  }

  public void deProvisionHosts(String appId, String infraMappingId, SettingAttribute computeProviderSetting,
      String region, List<String> hostNames) {
    AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);
    AmazonEC2Client amazonEc2Client =
        awsHelperService.getAmazonEc2Client(region, awsConfig.getAccessKey(), awsConfig.getSecretKey());

    List<AwsHost> awsHosts = hostService
                                 .list(aPageRequest()
                                           .addFilter("appId", Operator.EQ, appId)
                                           .addFilter("infraMappingId", Operator.EQ, infraMappingId)
                                           .addFilter("hostNames", Operator.IN, hostNames)
                                           .build())
                                 .getResponse();

    List<String> hostInstanceId =
        awsHosts.stream().map(host -> ((AwsHost) host).getInstance().getInstanceId()).collect(toList());
    TerminateInstancesResult terminateInstancesResult =
        amazonEc2Client.terminateInstances(new TerminateInstancesRequest(hostInstanceId));
  }

  private void waitForAllInstancesToBeReady(AmazonEC2Client amazonEC2Client, List<String> instancesIds) {
    Misc.quietSleep(1, TimeUnit.SECONDS);
    int retryCount = RETRY_COUNTER;
    while (!allInstanceInReadyState(amazonEC2Client, instancesIds)) {
      if (retryCount-- <= 0) {
        throw new WingsException(INIT_TIMEOUT, "message", "Not all instances in running state");
      }
      logger.info("Waiting for all instances to be in running state");
      Misc.sleepWithRuntimeException(SLEEP_INTERVAL);
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

  public List<LaunchConfiguration> listLaunchConfigurations(SettingAttribute computeProviderSetting, String region) {
    AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);
    AmazonAutoScalingClient amazonAutoScalingClient = awsHelperService.getAmazonAutoScalingClient(
        Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey());
    // TODO:: remove direct usage of LaunchConfiguration
    return amazonAutoScalingClient.describeLaunchConfigurations().getLaunchConfigurations();
  }

  public List<String> listClusterNames(SettingAttribute computeProviderSetting, String region) {
    AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);
    AmazonECSClient amazonEcsClient =
        awsHelperService.getAmazonEcsClient(region, awsConfig.getAccessKey(), awsConfig.getSecretKey());
    ListClustersResult listClustersResult = amazonEcsClient.listClusters(new ListClustersRequest());
    return listClustersResult.getClusterArns().stream().map(awsHelperService::getIdFromArn).collect(toList());
  }

  public List<String> listAMIs(SettingAttribute computeProviderSetting, String region) {
    AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);
    AmazonEC2Client amazonEC2Client =
        awsHelperService.getAmazonEc2Client(region, awsConfig.getAccessKey(), awsConfig.getSecretKey());
    List<String> imageIds = Lists.newArrayList(mainConfiguration.getAwsEcsAMIByRegion().get(region));
    imageIds.addAll(Lists
                        .newArrayList(amazonEC2Client
                                          .describeImages(new DescribeImagesRequest().withFilters(
                                              new Filter().withName("is-public").withValues("false"),
                                              new Filter().withName("state").withValues("available"),
                                              new Filter().withName("virtualization-type").withValues("hvm")))
                                          .getImages())
                        .stream()
                        .map(Image::getImageId)
                        .collect(toList()));
    return imageIds;
  }

  public List<String> listRegions(SettingAttribute computeProviderSetting) {
    AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);
    AmazonEC2Client amazonEC2Client = awsHelperService.getAmazonEc2Client(
        Regions.US_EAST_1.getName(), awsConfig.getAccessKey(), awsConfig.getSecretKey());
    return amazonEC2Client.describeRegions().getRegions().stream().map(Region::getRegionName).collect(toList());
  }

  public List<String> listInstanceTypes(SettingAttribute computeProviderSetting) {
    return mainConfiguration.getAwsInstanceTypes();
  }

  public List<String> listIAMInstanceRoles(SettingAttribute computeProviderSetting) {
    AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);
    AmazonIdentityManagementClient amazonIdentityManagementClient =
        awsHelperService.getAmazonIdentityManagementClient(awsConfig.getAccessKey(), awsConfig.getSecretKey());
    return amazonIdentityManagementClient.listInstanceProfiles()
        .getInstanceProfiles()
        .stream()
        .map(InstanceProfile::getInstanceProfileName)
        .collect(toList());
  }

  public Map<String, String> listIAMRoles(SettingAttribute computeProviderSetting) {
    AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);
    AmazonIdentityManagementClient amazonIdentityManagementClient =
        awsHelperService.getAmazonIdentityManagementClient(awsConfig.getAccessKey(), awsConfig.getSecretKey());

    return amazonIdentityManagementClient.listRoles(new ListRolesRequest().withMaxItems(400))
        .getRoles()
        .stream()
        .collect(Collectors.toMap(Role::getArn, Role::getRoleName));
  }

  public List<String> listVPCs(String region, SettingAttribute computeProviderSetting) {
    List<String> results = Lists.newArrayList();
    AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);
    AmazonEC2Client amazonEC2Client =
        awsHelperService.getAmazonEc2Client(region, awsConfig.getAccessKey(), awsConfig.getSecretKey());
    results.addAll(
        amazonEC2Client
            .describeVpcs(new DescribeVpcsRequest().withFilters(new Filter().withName("state").withValues("available"),
                new Filter().withName("isDefault").withValues("true")))
            .getVpcs()
            .stream()
            .map(Vpc::getVpcId)
            .collect(toList()));
    results.addAll(
        amazonEC2Client
            .describeVpcs(new DescribeVpcsRequest().withFilters(new Filter().withName("state").withValues("available"),
                new Filter().withName("isDefault").withValues("false")))
            .getVpcs()
            .stream()
            .map(Vpc::getVpcId)
            .collect(toList()));
    return results;
  }

  public List<String> listLoadBalancers(SettingAttribute computeProviderSetting, String region) {
    AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);

    AmazonElasticLoadBalancingClient amazonElasticLoadBalancingClient =
        awsHelperService.getAmazonElasticLoadBalancingClient(
            Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey());

    return amazonElasticLoadBalancingClient.describeLoadBalancers(new DescribeLoadBalancersRequest().withPageSize(400))
        .getLoadBalancers()
        .stream()
        .filter(loadBalancer -> StringUtils.equalsIgnoreCase(loadBalancer.getType(), "application"))
        .map(LoadBalancer::getLoadBalancerName)
        .collect(Collectors.toList());
  }

  public List<String> listClassicLoadBalancers(SettingAttribute computeProviderSetting, String region) {
    AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);

    List<LoadBalancerDescription> describeLoadBalancers =
        awsHelperService.getLoadBalancerDescriptions(region, awsConfig);
    return describeLoadBalancers.stream()
        .map(LoadBalancerDescription::getLoadBalancerName)
        .collect(Collectors.toList());
  }

  public List<String> listClassicLoadBalancers(String accessKey, char[] secretKey, String region) {
    com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient amazonElasticLoadBalancingClient =
        awsHelperService.getClassicElbClient(Regions.valueOf(region), accessKey, secretKey);

    return amazonElasticLoadBalancingClient
        .describeLoadBalancers(
            new com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersRequest().withPageSize(400))
        .getLoadBalancerDescriptions()
        .stream()
        .map(LoadBalancerDescription::getLoadBalancerName)
        .collect(Collectors.toList());
  }

  public Map<String, String> listTargetGroups(
      SettingAttribute computeProviderSetting, String region, String loadBalancerName) {
    AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);

    List<TargetGroup> targetGroups = awsHelperService.listTargetGroupsForElb(region, loadBalancerName, awsConfig);
    return targetGroups.stream().collect(
        Collectors.toMap(TargetGroup::getTargetGroupArn, TargetGroup::getTargetGroupName));
  }
}
