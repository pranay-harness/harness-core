package io.harness.delegate.beans.connector.gitconnector;

import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretReference;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class GitHTTPAuthenticationDTO extends GitAuthenticationDTO {
  @NotNull String username;
  @ApiModelProperty(dataType = "string") @NotNull @SecretReference SecretRefData passwordRef;
}
