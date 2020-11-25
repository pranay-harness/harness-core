package io.harness.delegate.beans.connector.nexusconnector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel("NexusAuthentication")
public class NexusAuthenticationDTO {
  @ApiModelProperty(allowableValues = NexusConstants.USERNAME_PASSWORD)
  @NotNull
  @JsonProperty("type")
  NexusAuthType authType;
  @JsonProperty("spec")
  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
  @JsonSubTypes(
      { @JsonSubTypes.Type(value = NexusUsernamePasswordAuthDTO.class, name = NexusConstants.USERNAME_PASSWORD) })
  @NotNull
  @Valid
  NexusAuthCredentialsDTO credentials;

  @Builder
  public NexusAuthenticationDTO(NexusAuthType authType, NexusAuthCredentialsDTO credentials) {
    this.authType = authType;
    this.credentials = credentials;
  }
}
