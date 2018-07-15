package software.wings.service.intfc.aws.manager;

import com.amazonaws.services.ec2.model.Instance;
import software.wings.beans.AwsConfig;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;

public interface AwsAsgHelperServiceManager {
  List<String> listAutoScalingGroupNames(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region);
  List<Instance> listAutoScalingGroupInstances(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String autoScalingGroupName);
}