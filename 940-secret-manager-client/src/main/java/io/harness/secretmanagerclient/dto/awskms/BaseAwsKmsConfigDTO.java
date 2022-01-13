/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.secretmanagerclient.dto.awskms;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.SecretManagerDescriptionConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.awskmsconnector.AwsKmsCredentialType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;

@OwnedBy(PL)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@ToString
@EqualsAndHashCode
@FieldNameConstants(innerTypeName = "BaseAwsKmsConfigDTOKeys")
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(name = "BaseAwsKmsConfig",
    description = "This contains the AWS KMS information as well as the metadata defined in Harness.")
public class BaseAwsKmsConfigDTO {
  @Schema(description = "This is the credential type that will be used to authenticate AWS KMS.")
  AwsKmsCredentialType credentialType;
  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "credentialType", include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
      visible = true)
  AwsKmsCredentialSpecConfig credential;
  @Schema(description = "This is the AWS KMS ARN.") String kmsArn;
  @Schema(description = "This is the AWS KMS Region.") String region;
  @Schema(description = SecretManagerDescriptionConstants.DELEGATE_SELECTORS) Set<String> delegateSelectors;
}
