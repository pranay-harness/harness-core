/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.beans.connector.awskmsconnector;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptableEntity;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import io.swagger.annotations.ApiModel;
import io.swagger.v3.oas.annotations.media.Schema;

@OwnedBy(PL)
@JsonSubTypes({
  @JsonSubTypes.Type(value = AwsKmsCredentialSpecManualConfigDTO.class, name = AwsKmsConstants.MANUAL_CONFIG)
  , @JsonSubTypes.Type(value = AwsKmsCredentialSpecAssumeIAMDTO.class, name = AwsKmsConstants.ASSUME_IAM_ROLE),
      @JsonSubTypes.Type(value = AwsKmsCredentialSpecAssumeSTSDTO.class, name = AwsKmsConstants.ASSUME_STS_ROLE)
})
@ApiModel("AwsKmsCredentialSpec")
@Schema(name = "AwsKmsCredentialSpec",
    description = "This is interface that returns credentials specific to all roles for the AWS KMS Secret Manager.")
public interface AwsKmsCredentialSpecDTO extends DecryptableEntity {}
