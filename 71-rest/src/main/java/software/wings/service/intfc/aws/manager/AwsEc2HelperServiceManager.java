package software.wings.service.intfc.aws.manager;

import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.ResourceType;
import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.beans.AwsConfig;
import software.wings.service.impl.aws.model.AwsSecurityGroup;
import software.wings.service.impl.aws.model.AwsSubnet;
import software.wings.service.impl.aws.model.AwsVPC;

import java.util.List;
import java.util.Set;

public interface AwsEc2HelperServiceManager {
  void validateAwsAccountCredential(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails);
  List<String> listRegions(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String appId);
  List<AwsVPC> listVPCs(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String appId);
  List<AwsSubnet> listSubnets(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region,
      List<String> vpcIds, String appId);
  List<AwsSecurityGroup> listSGs(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region,
      List<String> vpcIds, String appId);
  Set<String> listTags(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String appId);
  Set<String> listTags(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String appId,
      ResourceType resourceType);
  Set<String> listTags(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, ResourceType resourceType);
  List<Instance> listEc2Instances(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region,
      List<Filter> filters, String appId);
}