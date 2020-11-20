package software.wings.service.intfc.aws.manager;

import com.amazonaws.services.ec2.model.Instance;
import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.beans.AwsConfig;
import software.wings.service.impl.aws.model.AwsAsgGetRunningCountData;

import java.util.List;
import java.util.Map;

public interface AwsAsgHelperServiceManager {
  List<String> listAutoScalingGroupNames(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String appId);
  List<Instance> listAutoScalingGroupInstances(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String region, String autoScalingGroupName, String appId);
  Map<String, Integer> getDesiredCapacitiesOfAsgs(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, List<String> asgs, String appId);
  AwsAsgGetRunningCountData getCurrentlyRunningInstanceCount(AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, String region, String infraMappingId, String appId);
}
