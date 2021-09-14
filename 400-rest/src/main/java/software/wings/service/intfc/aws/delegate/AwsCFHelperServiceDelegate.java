/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.intfc.aws.delegate;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.AwsConfig;
import software.wings.beans.GitConfig;
import software.wings.beans.GitFileConfig;
import software.wings.service.impl.aws.model.AwsCFTemplateParamsData;

import java.util.List;

@TargetModule(HarnessModule._930_DELEGATE_TASKS)
@OwnedBy(CDP)
public interface AwsCFHelperServiceDelegate {
  String getStackBody(AwsConfig awsConfig, String region, String stackId);
  List<AwsCFTemplateParamsData> getParamsData(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String region, String data, String type, GitFileConfig gitFileConfig, GitConfig gitConfig,
      List<EncryptedDataDetail> sourceRepoEncryptedDetail);
  List<String> getCapabilities(AwsConfig awsConfig, String region, String data, String type);
}
