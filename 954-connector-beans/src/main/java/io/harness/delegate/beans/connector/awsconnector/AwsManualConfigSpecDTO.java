package io.harness.delegate.beans.connector.awsconnector;

import io.harness.beans.DecryptableEntity;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretReference;
import io.harness.validation.OneOfField;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonTypeName(AwsConstants.MANUAL_CONFIG)
@ApiModel("AwsManualConfigSpec")
@OneOfField(fields = {"accessKey", "accessKeyRef"})
@Schema(name = "AwsManualConfigSpec", description = "This contains AWS manual credentials connector spec")
public class AwsManualConfigSpecDTO implements AwsCredentialSpecDTO, DecryptableEntity {
  String accessKey;
  @ApiModelProperty(dataType = "string") @SecretReference SecretRefData accessKeyRef;
  @ApiModelProperty(dataType = "string") @NotNull @SecretReference SecretRefData secretKeyRef;
}
