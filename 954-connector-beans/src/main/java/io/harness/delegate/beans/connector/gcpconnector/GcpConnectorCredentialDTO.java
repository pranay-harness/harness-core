package io.harness.delegate.beans.connector.gcpconnector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModel;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel("GcpConnectorCredential")
public class GcpConnectorCredentialDTO {
  @NotNull @JsonProperty("type") GcpCredentialType gcpCredentialType;
  @JsonProperty("spec")
  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
  @NotNull
  @Valid
  GcpCredentialSpecDTO config;

  @Builder
  public GcpConnectorCredentialDTO(GcpCredentialType gcpCredentialType, GcpCredentialSpecDTO config) {
    this.gcpCredentialType = gcpCredentialType;
    this.config = config;
  }
}
