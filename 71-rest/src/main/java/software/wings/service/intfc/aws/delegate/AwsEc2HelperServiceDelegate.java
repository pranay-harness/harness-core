package software.wings.service.intfc.aws.delegate;

import com.amazonaws.services.ec2.model.CreateLaunchTemplateVersionRequest;
import com.amazonaws.services.ec2.model.CreateLaunchTemplateVersionResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.LaunchTemplateVersion;
import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.beans.AwsConfig;
import software.wings.service.impl.aws.model.AwsSecurityGroup;
import software.wings.service.impl.aws.model.AwsSubnet;
import software.wings.service.impl.aws.model.AwsVPC;

import java.util.List;
import java.util.Set;

public interface AwsEc2HelperServiceDelegate {
  boolean validateAwsAccountCredential(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails);
  List<String> listRegions(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails);
  List<AwsVPC> listVPCs(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region);
  List<AwsSubnet> listSubnets(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, List<String> vpcIds);
  List<AwsSecurityGroup> listSGs(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, List<String> vpcIds);
  Set<String> listTags(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String resourceType);
  List<Instance> listEc2Instances(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, List<Filter> filters);
  List<Instance> listEc2Instances(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, List<String> instanceIds, String region);
  Set<String> listBlockDeviceNamesOfAmi(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String amiId);

  LaunchTemplateVersion getLaunchTemplateVersion(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String region, String launchTemplateId, String version);

  CreateLaunchTemplateVersionResult createLaunchTemplateVersion(
      CreateLaunchTemplateVersionRequest createLaunchTemplateVersionRequest, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, String region);
}