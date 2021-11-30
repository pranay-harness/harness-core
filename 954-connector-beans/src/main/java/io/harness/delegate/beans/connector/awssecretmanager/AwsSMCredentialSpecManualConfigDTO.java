package io.harness.delegate.beans.connector.awssecretmanager;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretReference;

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
@JsonTypeName(AwsSecretManagerConstants.MANUAL_CONFIG)
@ApiModel("AwsSMCredentialSpecManualConfig")
@Schema(name = "AwsSMCredentialSpecManualConfig",
    description = "This contains the secret reference accessKey and secretKey of AWS SM")
public class AwsSMCredentialSpecManualConfigDTO implements AwsSecretManagerCredentialSpecDTO {
  @SecretReference @ApiModelProperty(dataType = "string") @NotNull private SecretRefData accessKey;
  @SecretReference @ApiModelProperty(dataType = "string") @NotNull private SecretRefData secretKey;
}