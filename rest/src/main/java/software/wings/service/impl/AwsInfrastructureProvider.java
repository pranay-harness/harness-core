package software.wings.service.impl;

import static java.util.stream.Collectors.toList;
import static software.wings.beans.AwsConfig.Builder.anAwsConfig;
import static software.wings.beans.ErrorCode.INIT_TIMEOUT;
import static software.wings.beans.ErrorCode.INVALID_ARGUMENT;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.infrastructure.AwsHost.Builder.anAwsHost;
import static software.wings.dl.PageRequest.Builder.aPageRequest;

import com.google.common.collect.Lists;

import ch.qos.logback.classic.Level;
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
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.identitymanagement.model.InstanceProfile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.configuration.ConfigurationFactory;
import io.dropwizard.configuration.ConfigurationFactoryFactory;
import io.dropwizard.configuration.DefaultConfigurationFactoryFactory;
import io.dropwizard.jackson.DiscoverableSubtypeResolver;
import io.dropwizard.jersey.validation.Validators;
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
import software.wings.utils.YamlUtils;

import java.io.File;
import java.io.IOException;
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
  private static final int SLEEP_INTERVAL = 30 * 1000;
  private static final int RETRY_COUNTER = (10 * 60 * 1000) / SLEEP_INTERVAL; // 10 minutes
  private final Logger logger = LoggerFactory.getLogger(AwsInfrastructureProvider.class);
  @Inject private AwsHelperService awsHelperService;
  @Inject private HostService hostService;
  @Inject private MainConfiguration mainConfiguration;

  public static void main(String... args) {
    ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(
        ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
    root.setLevel(Level.OFF);

    AwsInfrastructureProvider awsInfrastructureProvider = new AwsInfrastructureProvider();
    awsInfrastructureProvider.awsHelperService = new AwsHelperService();
    YamlUtils yamlUtils = new YamlUtils();
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.setSubtypeResolver(new DiscoverableSubtypeResolver());
    objectMapper.registerModule(new Jdk8Module());
    objectMapper.registerModule(new GuavaModule());
    try {
      ConfigurationFactoryFactory<MainConfiguration> configurationFactoryFactory =
          new DefaultConfigurationFactoryFactory<>();
      ConfigurationFactory<MainConfiguration> configurationFactory = configurationFactoryFactory.create(
          MainConfiguration.class, Validators.newValidatorFactory().getValidator(), objectMapper, "dw");
      MainConfiguration configuration = configurationFactory.build(new File("config.yml"));
      awsInfrastructureProvider.mainConfiguration = configuration;
    } catch (IOException e) {
      e.printStackTrace();
    } catch (ConfigurationException e) {
      e.printStackTrace();
    }

    SettingAttribute settingAttribute = aSettingAttribute()
                                            .withValue(anAwsConfig()
                                                           .withAccessKey("AKIAIJ5H5UG5TUB3L2QQ")
                                                           .withSecretKey("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")
                                                           .build())
                                            .build();
    System.out.println(awsInfrastructureProvider.listAMIs(settingAttribute, "us-east-1"));

    System.out.println(awsInfrastructureProvider.listRegions(settingAttribute));
    System.out.println(awsInfrastructureProvider.listIAMRoles(settingAttribute));
    System.out.println(awsInfrastructureProvider.listVPCs(settingAttribute));
    System.out.println(awsInfrastructureProvider.listInstanceTypes(settingAttribute));
  }

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
                              .collect(toList());
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
      int retryCount = 10;
      while (!awsHelperService.canConnectToHost(instance.getPublicDnsName(), 22, SLEEP_INTERVAL)) {
        if (retryCount-- <= 0) {
          throw new WingsException(INIT_TIMEOUT, "message", "Couldn't connect to provisioned host");
        }
      }
    }

    return amazonEc2Client.describeInstances(new DescribeInstancesRequest().withInstanceIds(instancesIds))
        .getReservations()
        .stream()
        .flatMap(reservation -> reservation.getInstances().stream())
        .map(instance -> anAwsHost().withHostName(instance.getPrivateDnsName()).withInstance(instance).build())
        .collect(toList());
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
        awsHosts.stream().map(host -> ((AwsHost) host).getInstance().getInstanceId()).collect(toList());
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
    return listClustersResult.getClusterArns().stream().map(awsHelperService::getIdFromArn).collect(toList());
  }

  public List<String> listAMIs(SettingAttribute computeProviderSetting, String region) {
    AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);
    AmazonEC2Client amazonEC2Client =
        awsHelperService.getAmazonEc2Client(awsConfig.getAccessKey(), awsConfig.getSecretKey());
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
    AmazonEC2Client amazonEC2Client =
        awsHelperService.getAmazonEc2Client(awsConfig.getAccessKey(), awsConfig.getSecretKey());
    return amazonEC2Client.describeRegions().getRegions().stream().map(Region::getRegionName).collect(toList());
  }

  public List<String> listInstanceTypes(SettingAttribute computeProviderSetting) {
    return mainConfiguration.getAwsInstanceTypes();
  }

  public List<String> listIAMRoles(SettingAttribute computeProviderSetting) {
    AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);
    AmazonIdentityManagementClient amazonIdentityManagementClient =
        awsHelperService.getAmazonIdentityManagementClient(awsConfig.getAccessKey(), awsConfig.getSecretKey());
    return amazonIdentityManagementClient.listInstanceProfiles()
        .getInstanceProfiles()
        .stream()
        .map(InstanceProfile::getInstanceProfileName)
        .collect(toList());
  }

  public List<String> listVPCs(SettingAttribute computeProviderSetting) {
    List<String> results = Lists.newArrayList();
    AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);
    AmazonEC2Client amazonEC2Client =
        awsHelperService.getAmazonEc2Client(awsConfig.getAccessKey(), awsConfig.getSecretKey());
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
}
