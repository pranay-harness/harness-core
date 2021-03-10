package io.harness.delegate.beans.connector.scm.awscodecommit;

import static io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitConnectorConstants.ACCESS_KEY_AND_SECRET_KEY;

import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretReference;
import io.harness.validation.OneOfField;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonTypeName(ACCESS_KEY_AND_SECRET_KEY)
@ApiModel("AwsCodeCommitSecretKeyAccessKeyDTO")
@OneOfField(fields = {"accessKey", "accessKeyRef"})
public class AwsCodeCommitSecretKeyAccessKeyDTO implements AwsCodeCommitHttpsCredentialsSpecDTO {
  String accessKey;
  @ApiModelProperty(dataType = "string") @SecretReference SecretRefData accessKeyRef;
  @ApiModelProperty(dataType = "string") @NotNull @SecretReference SecretRefData secretKeyRef;
}
