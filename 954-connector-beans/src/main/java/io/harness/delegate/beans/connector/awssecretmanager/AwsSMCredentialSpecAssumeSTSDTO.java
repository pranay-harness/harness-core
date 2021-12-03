package io.harness.delegate.beans.connector.awssecretmanager;

import static io.harness.annotations.dev.HarnessTeam.PL;

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
@Schema(name = "AwsSMCredentialSpecAssumeSTS", description = "This contains the credential spec of AWS SM for STS role")
public class AwsSMCredentialSpecAssumeSTSDTO implements AwsSecretManagerCredentialSpecDTO {
  @ApiModelProperty(dataType = "string") @NotNull private String roleArn;
  @ApiModelProperty(dataType = "string") private String externalId;
  private int assumeStsRoleDuration;
}