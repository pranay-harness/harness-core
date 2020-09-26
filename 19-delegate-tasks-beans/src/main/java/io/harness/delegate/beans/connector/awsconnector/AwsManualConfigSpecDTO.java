package io.harness.delegate.beans.connector.awsconnector;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.beans.DecryptableEntity;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretReference;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotNull;

@Value
@Builder
@JsonTypeName(AwsConstants.manualConfig)
public class AwsManualConfigSpecDTO implements AwsCredentialSpecDTO, DecryptableEntity {
  @NotNull String accessKey;
  @ApiModelProperty(dataType = "string") @NotNull @SecretReference SecretRefData secretKeyRef;
}
