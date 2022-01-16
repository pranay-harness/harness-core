/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.beans.connector.awssecretmanager;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.SecretManagerDescriptionConstants;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@OwnedBy(PL)
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(AwsSecretManagerConstants.ASSUME_STS_ROLE)
@ApiModel("AwsSMCredentialSpecAssumeSTS")
@Schema(name = "AwsSMCredentialSpecAssumeSTS",
    description = "Returns credentials for the AWS Secret Manager for the IAM role.")
public class AwsSMCredentialSpecAssumeSTSDTO implements AwsSecretManagerCredentialSpecDTO {
  @ApiModelProperty(dataType = "string")
  @NotNull
  @Schema(description = SecretManagerDescriptionConstants.ROLE_ARN)
  private String roleArn;
  @ApiModelProperty(dataType = "string")
  @Schema(description = SecretManagerDescriptionConstants.EXTERNAL_NAME)
  private String externalId;
  @Schema(description = SecretManagerDescriptionConstants.ASSUME_STS_ROLE_DURATION) private int assumeStsRoleDuration;
}
