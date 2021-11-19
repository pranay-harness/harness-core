package io.harness.delegate.beans.connector.nexusconnector;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.annotations.ApiModel;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@OwnedBy(CDC)
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel("NexusAuthentication")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(using = NexusAuthDTODeserializer.class)
@Schema(name = "NexusAuthentication", description = "This contains details of Nexus Authentication")
public class NexusAuthenticationDTO {
  @NotNull @JsonProperty("type") NexusAuthType authType;
  @JsonProperty("spec")
  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
  @Valid
  NexusAuthCredentialsDTO credentials;

  @Builder
  public NexusAuthenticationDTO(NexusAuthType authType, NexusAuthCredentialsDTO credentials) {
    this.authType = authType;
    this.credentials = credentials;
  }
}
