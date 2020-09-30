package io.harness.delegate.beans.connector.gcpconnector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModel;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

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
  @JsonSubTypes({
    @JsonSubTypes.Type(value = GcpDelegateDetailsDTO.class, name = GcpConstants.INHERIT_FROM_DELEGATE)
    , @JsonSubTypes.Type(value = GcpManualDetailsDTO.class, name = GcpConstants.MANUAL_CONFIG)
  })
  @NotNull
  @Valid
  GcpCredentialSpecDTO config;

  @Builder
  public GcpConnectorCredentialDTO(GcpCredentialType gcpCredentialType, GcpCredentialSpecDTO config) {
    this.gcpCredentialType = gcpCredentialType;
    this.config = config;
  }
}
