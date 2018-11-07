package software.wings.service.intfc.aws.manager;

import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.ResourceType;
import software.wings.beans.AwsConfig;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;
import java.util.Set;

public interface AwsEc2HelperServiceManager {
  void validateAwsAccountCredential(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails);
  List<String> listRegions(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String appId);
  List<String> listVPCs(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String appId);
  List<String> listSubnets(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region,
      List<String> vpcIds, String appId);
  List<String> listSGs(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region,
      List<String> vpcIds, String appId);
  Set<String> listTags(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String appId);
  Set<String> listTags(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String appId,
      ResourceType resourceType);
  List<Instance> listEc2Instances(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region,
      List<Filter> filters, String appId);
}