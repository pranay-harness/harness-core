/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.secretmanagerclient.dto.awssecretmanager;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.awssecretmanager.AwsSecretManagerConstants;

import com.fasterxml.jackson.annotation.JsonSubTypes;

@OwnedBy(PL)
@JsonSubTypes({
  @JsonSubTypes.Type(value = AwsSMStsCredentialConfig.class, name = AwsSecretManagerConstants.ASSUME_STS_ROLE)
  , @JsonSubTypes.Type(value = AwsSMIamRoleCredentialConfig.class, name = AwsSecretManagerConstants.ASSUME_IAM_ROLE),
      @JsonSubTypes.Type(value = AwsSMManualCredentialConfig.class, name = AwsSecretManagerConstants.MANUAL_CONFIG)
})
public interface AwsSMCredentialSpecConfig {}
