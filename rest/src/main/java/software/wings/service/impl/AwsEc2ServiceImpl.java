package software.wings.service.impl;

import static java.util.stream.Collectors.toList;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import software.wings.beans.AwsConfig;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.AwsEc2Service;

import java.util.List;

/**
 * Created by anubhaw on 6/17/18.
 */
@Singleton
public class AwsEc2ServiceImpl implements AwsEc2Service {
  @Inject private AwsHelperService awsHelperService;

  @Override
  public List<Instance> describeAutoScalingGroupInstances(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String autoScalingGroupName) {
    return getInstanceList(
        awsHelperService.describeAutoScalingGroupInstances(awsConfig, encryptionDetails, region, autoScalingGroupName));
  }

  private List<Instance> getInstanceList(DescribeInstancesResult result) {
    List<Instance> instanceList = Lists.newArrayList();
    result.getReservations().forEach(reservation -> instanceList.addAll(reservation.getInstances()));
    return instanceList;
  }

  @Override
  public List<String> getAutoScalingGroupNames(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region) {
    List<AutoScalingGroup> groups = awsHelperService.listAutoScalingGroups(awsConfig, encryptionDetails, region);
    return groups.stream().map(AutoScalingGroup::getAutoScalingGroupName).collect(toList());
  }
}