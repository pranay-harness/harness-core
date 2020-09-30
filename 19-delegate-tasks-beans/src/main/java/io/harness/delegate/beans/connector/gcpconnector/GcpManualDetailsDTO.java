package io.harness.delegate.beans.connector.gcpconnector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.beans.DecryptableEntity;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretReference;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel("GcpManualDetails")
public class GcpManualDetailsDTO implements GcpCredentialSpecDTO, DecryptableEntity {
  @ApiModelProperty(dataType = "string") @NotNull @SecretReference SecretRefData secretKeyRef;
}
