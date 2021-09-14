/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.aws.model;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.AwsConfig;

import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
@OwnedBy(CDP)
public class AwsLambdaRequest extends AwsRequest {
  public enum AwsLambdaRequestType {
    EXECUTE_LAMBDA_WF,
    EXECUTE_LAMBDA_FUNCTION,
    LIST_LAMBDA_FUNCTION,
    LAMBDA_FUNCTION_DETAILS
  }

  @NotNull private AwsLambdaRequestType requestType;
  @NotNull private String region;

  public AwsLambdaRequest(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      AwsLambdaRequestType requestType, String region) {
    super(awsConfig, encryptionDetails);
    this.requestType = requestType;
    this.region = region;
  }
}
