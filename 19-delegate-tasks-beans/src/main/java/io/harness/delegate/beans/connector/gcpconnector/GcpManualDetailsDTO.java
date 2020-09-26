package io.harness.delegate.beans.connector.gcpconnector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(GcpConstants.manualConfig)
public class GcpManualDetailsDTO implements GcpCredentialSpecDTO {
  @JsonProperty("spec") @NotNull @Valid GcpSecretKeyAuthDTO gcpSecretKeyAuthDTO;
}
