package software.wings.service.impl;

import static java.util.stream.Collectors.toList;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ecr.model.Repository;
import com.amazonaws.services.ecs.model.ListClustersRequest;
import com.amazonaws.services.ecs.model.ListClustersResult;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetGroup;
import software.wings.beans.AwsConfig;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.AwsEc2Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by anubhaw on 6/17/18.
 */
@Singleton
public class AwsEc2ServiceImpl implements AwsEc2Service {
  @Inject private AwsHelperService awsHelperService;

  @Override
  public boolean validateAwsAccountCredential(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails) {
    return awsHelperService.validateAwsAccountCredential(awsConfig, encryptionDetails);
  }

  @Override
  public List<String> getRegions(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails) {
    return awsHelperService.listRegions(awsConfig, encryptionDetails);
  }

  @Override
  public List<String> getClusters(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region) {
    ListClustersResult listClustersResult =
        awsHelperService.listClusters(region, awsConfig, encryptionDetails, new ListClustersRequest());
    return listClustersResult.getClusterArns().stream().map(awsHelperService::getIdFromArn).collect(toList());
  }

  @Override
  public List<String> getVPCs(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region) {
    return awsHelperService.listVPCs(awsConfig, encryptionDetails, region);
  }

  @Override
  public Map<String, String> getIAMRoles(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails) {
    return awsHelperService.listIAMRoles(awsConfig, encryptionDetails);
  }

  @Override
  public String getEcrImageUrl(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String imageName) {
    Repository repository = awsHelperService.getRepository(awsConfig, encryptionDetails, region, imageName);
    return repository != null ? repository.getRepositoryUri() : null;
  }

  @Override
  public String getAmazonEcrAuthToken(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String awsAccount, String region) {
    return awsHelperService.getAmazonEcrAuthToken(awsConfig, encryptionDetails, awsAccount, region);
  }

  @Override
  public List<String> getSubnets(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, List<String> vpcIds) {
    return awsHelperService.listSubnetIds(awsConfig, encryptionDetails, region, vpcIds);
  }

  @Override
  public List<String> getSGs(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, List<String> vpcIds) {
    return awsHelperService.listSecurityGroupIds(awsConfig, encryptionDetails, region, vpcIds);
  }

  @Override
  public Set<String> getTags(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region) {
    return awsHelperService.listTags(awsConfig, encryptionDetails, region);
  }

  @Override
  public List<Instance> describeAutoScalingGroupInstances(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String autoScalingGroupName) {
    return getInstanceList(
        awsHelperService.describeAutoScalingGroupInstances(awsConfig, encryptionDetails, region, autoScalingGroupName));
  }

  @Override
  public List<Instance> describeEc2Instances(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, List<Filter> filters) {
    return getInstanceList(awsHelperService.describeEc2Instances(
        awsConfig, encryptionDetails, region, new DescribeInstancesRequest().withFilters(filters)));
  }

  private List<Instance> getInstanceList(DescribeInstancesResult result) {
    List<Instance> instanceList = Lists.newArrayList();
    result.getReservations().forEach(reservation -> instanceList.addAll(reservation.getInstances()));
    return instanceList;
  }

  @Override
  public List<String> getIAMInstanceRoles(AwsConfig awsConfig) {
    return awsHelperService.listIAMInstanceRoles(awsConfig);
  }

  @Override
  public List<String> getApplicationLoadBalancers(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region) {
    return awsHelperService.listApplicationLoadBalancers(awsConfig, encryptionDetails, region);
  }

  @Override
  public List<String> getClassicLoadBalancers(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region) {
    return awsHelperService.listClassicLoadBalancers(awsConfig, encryptionDetails, region);
  }

  @Override
  public Map<String, String> getTargetGroupsForAlb(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String loadBalancerName) {
    List<TargetGroup> targetGroups =
        awsHelperService.listTargetGroupsForAlb(region, awsConfig, encryptionDetails, loadBalancerName);
    return targetGroups.stream().collect(
        Collectors.toMap(TargetGroup::getTargetGroupArn, TargetGroup::getTargetGroupName));
  }

  @Override
  public List<String> getAutoScalingGroupNames(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region) {
    List<AutoScalingGroup> groups = awsHelperService.listAutoScalingGroups(awsConfig, encryptionDetails, region);
    return groups.stream().map(AutoScalingGroup::getAutoScalingGroupName).collect(toList());
  }
}
