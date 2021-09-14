/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.connector.awskmsconnector;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptableEntity;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import io.swagger.annotations.ApiModel;

@OwnedBy(PL)
@JsonSubTypes({
  @JsonSubTypes.Type(value = AwsKmsCredentialSpecManualConfigDTO.class, name = AwsKmsConstants.MANUAL_CONFIG)
  , @JsonSubTypes.Type(value = AwsKmsCredentialSpecAssumeIAMDTO.class, name = AwsKmsConstants.ASSUME_IAM_ROLE),
      @JsonSubTypes.Type(value = AwsKmsCredentialSpecAssumeSTSDTO.class, name = AwsKmsConstants.ASSUME_STS_ROLE)
})
@ApiModel("AwsKmsCredentialSpec")
public interface AwsKmsCredentialSpecDTO extends DecryptableEntity {}
