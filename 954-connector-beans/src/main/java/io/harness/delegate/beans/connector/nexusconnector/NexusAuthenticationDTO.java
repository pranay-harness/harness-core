package io.harness.delegate.beans.connector.nexusconnector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.annotations.ApiModel;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel("NexusAuthentication")
@JsonDeserialize(using = NexusAuthDTODeserializer.class)
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
