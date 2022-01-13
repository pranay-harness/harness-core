/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.intfc.aws.manager;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.AwsConfig;
import software.wings.service.impl.aws.model.AwsSecurityGroup;
import software.wings.service.impl.aws.model.AwsSubnet;
import software.wings.service.impl.aws.model.AwsVPC;

import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.ResourceType;
import java.util.List;
import java.util.Set;

@OwnedBy(CDP)
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
