/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.connector.awssecretmanager;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptableEntity;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import io.swagger.annotations.ApiModel;

@OwnedBy(PL)
@JsonSubTypes({
  @JsonSubTypes.Type(value = AwsSMCredentialSpecManualConfigDTO.class, name = AwsSecretManagerConstants.MANUAL_CONFIG)
  , @JsonSubTypes.Type(value = AwsSMCredentialSpecAssumeIAMDTO.class, name = AwsSecretManagerConstants.ASSUME_IAM_ROLE),
      @JsonSubTypes.Type(
          value = AwsSMCredentialSpecAssumeSTSDTO.class, name = AwsSecretManagerConstants.ASSUME_STS_ROLE)
})
@ApiModel("AwsSecretManagerCredentialSpec")
public interface AwsSecretManagerCredentialSpecDTO extends DecryptableEntity {}
