package software.wings.service.intfc.aws.delegate;

import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import software.wings.beans.AwsConfig;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;
import java.util.Set;

public interface AwsEc2HelperServiceDelegate {
  boolean validateAwsAccountCredential(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails);
  List<String> listRegions(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails);
  List<String> listVPCs(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region);
  List<String> listSubnets(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, List<String> vpcIds);
  List<String> listSGs(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, List<String> vpcIds);
  Set<String> listTags(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String resourceType);
  List<Instance> listEc2Instances(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, List<Filter> filters);
  List<Instance> listEc2Instances(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, List<String> instanceIds, String region);
}