/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.connector.awskmsconnector;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.Set;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@OwnedBy(PL)
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(AwsKmsConstants.ASSUME_STS_ROLE)
@ApiModel("AwsKmsCredentialSpecAssumeSTS")
public class AwsKmsCredentialSpecAssumeSTSDTO implements AwsKmsCredentialSpecDTO {
  @NotNull @Size(min = 1, message = "Delegate Selector can not be empty") private Set<String> delegateSelectors;
  @ApiModelProperty(dataType = "string") @NotNull private String roleArn;
  @ApiModelProperty(dataType = "string") private String externalName;
  private int assumeStsRoleDuration;
}
