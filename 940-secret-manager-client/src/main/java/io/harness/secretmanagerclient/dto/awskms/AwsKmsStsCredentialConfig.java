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

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(PL)
@Getter
@Setter
@Builder
@FieldNameConstants(innerTypeName = "AwsKmsStsCredentialConfigKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@TypeAlias("io.harness.secretmanagerclient.dto.awskms.AwsKmsStsCredentialConfig")
@Schema(
    description =
        "This contains the specifications of the AWS KMS credential, which uses the Delegate with STS role to authenticate.")
public class AwsKmsStsCredentialConfig implements AwsKmsCredentialSpecConfig {
  @Schema(description = SecretManagerDescriptionConstants.DELEGATE_SELECTORS) Set<String> delegateSelectors;
  @Schema(description = SecretManagerDescriptionConstants.ROLE_ARN) String roleArn;
  @Schema(description = SecretManagerDescriptionConstants.EXTERNAL_NAME) String externalName;
  @Schema(description = SecretManagerDescriptionConstants.ASSUME_STS_ROLE_DURATION) int assumeStsRoleDuration;
}
