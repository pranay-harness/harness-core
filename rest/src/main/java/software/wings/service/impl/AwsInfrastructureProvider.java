package software.wings.service.impl;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static software.wings.beans.ErrorCode.INVALID_ARGUMENT;
import static software.wings.beans.infrastructure.Host.Builder.aHost;
import static software.wings.dl.PageResponse.Builder.aPageResponse;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.codedeploy.model.AmazonCodeDeployException;
import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Image;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ecs.model.AmazonECSException;
import com.amazonaws.services.ecs.model.ListClustersRequest;
import com.amazonaws.services.ecs.model.ListClustersResult;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.app.MainConfiguration;
import software.wings.beans.AwsConfig;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.AwsInstanceFilter;
import software.wings.beans.ErrorCode;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.beans.infrastructure.Host;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.exception.WingsException;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.InfrastructureProvider;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.states.AwsAmiServiceDeployState.ExecutionLogCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by anubhaw on 10/4/16.
 */
@Singleton
public class AwsInfrastructureProvider implements InfrastructureProvider {
  private static final Logger logger = LoggerFactory.getLogger(AwsInfrastructureProvider.class);
  @Inject private AwsHelperService awsHelperService;
  @Inject private HostService hostService;
  @Inject private MainConfiguration mainConfiguration;
  @Inject private SecretManager secretManager;

  @Override
  public PageResponse<Host> listHosts(AwsInfrastructureMapping awsInfrastructureMapping,
      SettingAttribute computeProviderSetting, List<EncryptedDataDetail> encryptedDataDetails, PageRequest<Host> req) {
    AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);
    DescribeInstancesResult describeInstancesResult;
    if (awsInfrastructureMapping.isProvisionInstances()) {
      describeInstancesResult = listAutoScaleHosts(awsInfrastructureMapping, awsConfig, encryptedDataDetails);
    } else {
      describeInstancesResult = listFilteredHosts(awsInfrastructureMapping, awsConfig, encryptedDataDetails);
    }
    if (describeInstancesResult != null && describeInstancesResult.getReservations() != null) {
      List<Host> awsHosts =
          describeInstancesResult.getReservations()
              .stream()
              .flatMap(reservation -> reservation.getInstances().stream())
              .map(instance
                  -> aHost()
                         .withHostName(awsHelperService.getHostnameFromPrivateDnsName(instance.getPrivateDnsName()))
                         .withPublicDns(awsInfrastructureMapping.isUsePublicDns() ? instance.getPublicDnsName()
                                                                                  : instance.getPrivateDnsName())
                         .withEc2Instance(instance)
                         .build())
              .collect(toList());
      return aPageResponse().withResponse(awsHosts).build();
    }
    return aPageResponse().withResponse(emptyList()).build();
  }

  private DescribeInstancesResult listAutoScaleHosts(AwsInfrastructureMapping awsInfrastructureMapping,
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    DescribeInstancesResult describeInstancesResult;
    if (awsInfrastructureMapping.isSetDesiredCapacity()) {
      List<Instance> instances = new ArrayList<>();
      for (int i = 0; i < awsInfrastructureMapping.getDesiredCapacity(); i++) {
        int instanceNum = i + 1;
        instances.add(new Instance()
                          .withPrivateDnsName("private-dns-" + instanceNum)
                          .withPublicDnsName("public-dns-" + instanceNum));
      }
      describeInstancesResult =
          new DescribeInstancesResult().withReservations(new Reservation().withInstances(instances));
    } else {
      describeInstancesResult = awsHelperService.describeAutoScalingGroupInstances(awsConfig, encryptedDataDetails,
          awsInfrastructureMapping.getRegion(), awsInfrastructureMapping.getAutoScalingGroupName());
    }
    return describeInstancesResult;
  }

  private DescribeInstancesResult listFilteredHosts(AwsInfrastructureMapping awsInfrastructureMapping,
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    AwsInstanceFilter instanceFilter = awsInfrastructureMapping.getAwsInstanceFilter();
    List<Filter> filters = new ArrayList<>();
    filters.add(new Filter("instance-state-name").withValues("running"));
    if (instanceFilter != null) {
      if (isNotEmpty(instanceFilter.getVpcIds())) {
        filters.add(new Filter("vpc-id", instanceFilter.getVpcIds()));
      }
      if (isNotEmpty(instanceFilter.getSecurityGroupIds())) {
        filters.add(new Filter("instance.group-id", instanceFilter.getSecurityGroupIds()));
      }
      if (isNotEmpty(instanceFilter.getSubnetIds())) {
        filters.add(new Filter("network-interface.subnet-id", instanceFilter.getSubnetIds()));
      }
      if (isNotEmpty(instanceFilter.getTags())) {
        Multimap<String, String> tags = ArrayListMultimap.create();
        instanceFilter.getTags().forEach(tag -> tags.put(tag.getKey(), tag.getValue()));
        tags.keySet().forEach(key -> filters.add(new Filter("tag:" + key, new ArrayList<>(tags.get(key)))));
      }
    }
    return awsHelperService.describeEc2Instances(awsConfig, encryptedDataDetails, awsInfrastructureMapping.getRegion(),
        new DescribeInstancesRequest().withFilters(filters));
  }

  @Override
  public void deleteHost(String appId, String infraMappingId, String dnsName) {
    hostService.deleteByDnsName(appId, infraMappingId, dnsName);
  }

  @Override
  public void updateHostConnAttrs(InfrastructureMapping infrastructureMapping, String hostConnectionAttrs) {
    hostService.updateHostConnectionAttrByInfraMappingId(
        infrastructureMapping.getAppId(), infrastructureMapping.getUuid(), hostConnectionAttrs);
  }

  private AwsConfig validateAndGetAwsConfig(SettingAttribute computeProviderSetting) {
    if (computeProviderSetting == null || !(computeProviderSetting.getValue() instanceof AwsConfig)) {
      throw new WingsException(INVALID_ARGUMENT).addParam("args", "InvalidConfiguration");
    }

    return (AwsConfig) computeProviderSetting.getValue();
  }

  @Override
  public Host saveHost(Host host) {
    return hostService.saveHost(host);
  }

  public List<Host> maybeSetAutoScaleCapacityAndGetHosts(String appId, String workflowExecutionId,
      AwsInfrastructureMapping infrastructureMapping, SettingAttribute computeProviderSetting) {
    AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);
    List<EncryptedDataDetail> encryptionDetails =
        secretManager.getEncryptionDetails(awsConfig, appId, workflowExecutionId);

    if (infrastructureMapping.isSetDesiredCapacity()) {
      awsHelperService.setAutoScalingGroupCapacityAndWaitForInstancesReadyState(awsConfig, encryptionDetails,
          infrastructureMapping.getRegion(), infrastructureMapping.getAutoScalingGroupName(),
          infrastructureMapping.getDesiredCapacity(), new ExecutionLogCallback());
    }
    List<String> instancesIds = awsHelperService.listInstanceIdsFromAutoScalingGroup(awsConfig, encryptionDetails,
        infrastructureMapping.getRegion(), infrastructureMapping.getAutoScalingGroupName());
    logger.info("Got {} instance ids from auto scaling group {}: {}", instancesIds.size(),
        infrastructureMapping.getAutoScalingGroupName(), instancesIds);

    return awsHelperService
        .describeEc2Instances(awsConfig, encryptionDetails, infrastructureMapping.getRegion(),
            new DescribeInstancesRequest().withInstanceIds(instancesIds))
        .getReservations()
        .stream()
        .flatMap(reservation -> reservation.getInstances().stream())
        .map(instance
            -> aHost()
                   .withHostName(awsHelperService.getHostnameFromPrivateDnsName(instance.getPrivateDnsName()))
                   .withPublicDns(infrastructureMapping.isUsePublicDns() ? instance.getPublicDnsName()
                                                                         : instance.getPrivateDnsName())
                   .withEc2Instance(instance)
                   .build())
        .collect(toList());
  }

  public List<String> listClusterNames(SettingAttribute computeProviderSetting, String region) {
    AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);
    List<EncryptedDataDetail> encryptionDetails = secretManager.getEncryptionDetails(awsConfig, null, null);
    try {
      ListClustersResult listClustersResult =
          awsHelperService.listClusters(region, awsConfig, encryptionDetails, new ListClustersRequest());
      return listClustersResult.getClusterArns().stream().map(awsHelperService::getIdFromArn).collect(toList());
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new ArrayList<>();
  }

  public List<String> listAMIs(SettingAttribute computeProviderSetting, String region) {
    AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);
    try {
      List<String> imageIds = Lists.newArrayList(mainConfiguration.getAwsEcsAMIByRegion().get(region));
      imageIds.addAll(
          Lists
              .newArrayList(
                  awsHelperService
                      .decribeEc2Images(awsConfig, secretManager.getEncryptionDetails(awsConfig, null, null), region,
                          new DescribeImagesRequest().withFilters(new Filter("is-public").withValues("false"),
                              new Filter("state").withValues("available"),
                              new Filter("virtualization-type").withValues("hvm")))
                      .getImages())
              .stream()
              .map(Image::getImageId)
              .collect(toList()));
      return imageIds;
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new ArrayList<>();
  }

  public List<String> listRegions(SettingAttribute computeProviderSetting) {
    AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);
    return awsHelperService.listRegions(awsConfig, secretManager.getEncryptionDetails(awsConfig, null, null));
  }

  public List<String> listInstanceTypes(SettingAttribute computeProviderSetting) {
    return mainConfiguration.getAwsInstanceTypes();
  }

  public List<String> listIAMInstanceRoles(SettingAttribute computeProviderSetting) {
    AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);
    return awsHelperService.listIAMInstanceRoles(awsConfig);
  }

  public Map<String, String> listIAMRoles(SettingAttribute computeProviderSetting) {
    AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);
    return awsHelperService.listIAMRoles(awsConfig, secretManager.getEncryptionDetails(awsConfig, null, null));
  }

  public List<String> listVPCs(SettingAttribute computeProviderSetting, String region) {
    AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);
    return awsHelperService.listVPCs(awsConfig, secretManager.getEncryptionDetails(awsConfig, null, null), region);
  }

  public List<String> listSecurityGroups(SettingAttribute computeProviderSetting, String region, List<String> vpcIds) {
    AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);
    return awsHelperService.listSecurityGroupIds(
        awsConfig, secretManager.getEncryptionDetails(awsConfig, null, null), region, vpcIds);
  }

  public List<String> listSubnets(SettingAttribute computeProviderSetting, String region, List<String> vpcIds) {
    AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);
    return awsHelperService.listSubnetIds(
        awsConfig, secretManager.getEncryptionDetails(awsConfig, null, null), region, vpcIds);
  }

  public List<String> listLoadBalancers(SettingAttribute computeProviderSetting, String region) {
    AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);
    return awsHelperService.listApplicationLoadBalancers(
        awsConfig, secretManager.getEncryptionDetails(awsConfig, null, null), region);
  }

  public List<String> listClassicLoadBalancers(SettingAttribute computeProviderSetting, String region) {
    AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);
    return awsHelperService.listClassicLoadBalancers(
        awsConfig, secretManager.getEncryptionDetails(awsConfig, null, null), region);
  }

  public List<String> listClassicLoadBalancers(String accessKey, char[] secretKey, String region) {
    AwsConfig awsConfig = AwsConfig.builder().accessKey(accessKey).secretKey(secretKey).build();
    return awsHelperService.listApplicationLoadBalancers(
        awsConfig, secretManager.getEncryptionDetails(awsConfig, null, null), region);
  }

  public Map<String, String> listTargetGroups(
      SettingAttribute computeProviderSetting, String region, String loadBalancerName) {
    AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);
    try {
      List<TargetGroup> targetGroups = awsHelperService.listTargetGroupsForAlb(
          region, awsConfig, secretManager.getEncryptionDetails(awsConfig, null, null), loadBalancerName);
      return targetGroups.stream().collect(
          Collectors.toMap(TargetGroup::getTargetGroupArn, TargetGroup::getTargetGroupName));
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return Maps.newHashMap();
  }

  private void handleAmazonServiceException(AmazonServiceException amazonServiceException) {
    logger.error("AWS API call exception", amazonServiceException);
    if (amazonServiceException instanceof AmazonCodeDeployException) {
      throw new WingsException(ErrorCode.AWS_ACCESS_DENIED, new Throwable(amazonServiceException.getErrorMessage()));
    } else if (amazonServiceException instanceof AmazonEC2Exception) {
      throw new WingsException(ErrorCode.AWS_ACCESS_DENIED)
          .addParam("message", amazonServiceException.getErrorMessage());
    } else if (amazonServiceException instanceof AmazonECSException) {
      throw new WingsException(ErrorCode.AWS_ACCESS_DENIED)
          .addParam("message", amazonServiceException.getErrorMessage());
    }
    logger.error("Unhandled aws exception");
    throw new WingsException(ErrorCode.ACCESS_DENIED).addParam("message", amazonServiceException.getErrorMessage());
  }

  public Set<String> listTags(SettingAttribute computeProviderSetting, String region) {
    AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);
    return awsHelperService.listTags(awsConfig, secretManager.getEncryptionDetails(awsConfig, null, null), region);
  }

  public List<String> listAutoScalingGroups(SettingAttribute computeProviderSetting, String region) {
    AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);
    return awsHelperService
        .listAutoScalingGroups(awsConfig, secretManager.getEncryptionDetails(awsConfig, null, null), region)
        .stream()
        .map(AutoScalingGroup::getAutoScalingGroupName)
        .collect(Collectors.toList());
  }
}
