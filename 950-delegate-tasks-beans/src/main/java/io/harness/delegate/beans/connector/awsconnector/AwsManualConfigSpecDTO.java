package io.harness.delegate.beans.connector.awsconnector;

import io.harness.beans.DecryptableEntity;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretReference;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonTypeName(AwsConstants.MANUAL_CONFIG)
@ApiModel("AwsManualConfigSpec")
public class AwsManualConfigSpecDTO implements AwsCredentialSpecDTO, DecryptableEntity {
  @NotNull String accessKey;
  @ApiModelProperty(dataType = "string") @NotNull @SecretReference SecretRefData secretKeyRef;
}
