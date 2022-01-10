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
import software.wings.service.impl.aws.model.AwsCodeDeployS3LocationData;

import com.amazonaws.services.ec2.model.Instance;
import java.util.List;

@OwnedBy(CDP)
public interface AwsCodeDeployHelperServiceManager {
  List<String> listApplications(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String appId);
  List<String> listDeploymentConfiguration(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptedDataDetails, String region, String appId);
  List<String> listDeploymentGroups(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptedDataDetails, String region, String appName, String appId);
  List<Instance> listDeploymentInstances(AwsConfig awsConfig, List<EncryptedDataDetail> encryptedDataDetails,
      String region, String deploymentId, String appId);
  AwsCodeDeployS3LocationData listAppRevision(AwsConfig awsConfig, List<EncryptedDataDetail> encryptedDataDetails,
      String region, String appName, String deploymentGroupName, String appId);
}
