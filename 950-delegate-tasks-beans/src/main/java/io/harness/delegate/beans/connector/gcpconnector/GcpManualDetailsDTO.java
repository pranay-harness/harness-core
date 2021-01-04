package io.harness.delegate.beans.connector.gcpconnector;

import static io.harness.yamlSchema.NGSecretReferenceConstants.SECRET_REF_PATTERN;

import io.harness.beans.DecryptableEntity;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretReference;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(GcpConstants.MANUAL_CONFIG)
@ApiModel("GcpManualDetails")
public class GcpManualDetailsDTO implements GcpCredentialSpecDTO, DecryptableEntity {
  @ApiModelProperty(dataType = "string")
  @NotNull
  @SecretReference
  @Pattern(regexp = SECRET_REF_PATTERN)
  SecretRefData secretKeyRef;
}
