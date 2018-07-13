package software.wings.service.intfc;

import com.amazonaws.services.ec2.model.Instance;
import software.wings.beans.AwsConfig;
import software.wings.beans.TaskType;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;

/**
 * Created by anubhaw on 6/17/18.
 */
public interface AwsEc2Service {
  @DelegateTaskType(TaskType.AWS_DESCRIBE_AUTO_SCALING_GROUP_INSTANCES)
  List<Instance> describeAutoScalingGroupInstances(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String autoScalingGroupName);

  @DelegateTaskType(TaskType.AWS_GET_AUTO_SCALING_GROUPS)
  List<String> getAutoScalingGroupNames(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region);
}