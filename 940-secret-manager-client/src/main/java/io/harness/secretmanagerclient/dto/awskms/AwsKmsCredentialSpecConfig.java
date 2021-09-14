/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.secretmanagerclient.dto.awskms;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.awskmsconnector.AwsKmsConstants;

import com.fasterxml.jackson.annotation.JsonSubTypes;

@OwnedBy(PL)
@JsonSubTypes({
  @JsonSubTypes.Type(value = AwsKmsStsCredentialConfig.class, name = AwsKmsConstants.ASSUME_STS_ROLE)
  , @JsonSubTypes.Type(value = AwsKmsIamCredentialConfig.class, name = AwsKmsConstants.ASSUME_IAM_ROLE),
      @JsonSubTypes.Type(value = AwsKmsManualCredentialConfig.class, name = AwsKmsConstants.MANUAL_CONFIG)
})
public interface AwsKmsCredentialSpecConfig {}
