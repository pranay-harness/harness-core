package io.harness.delegate.beans.connector.artifactoryconnector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel("ArtifactoryAuthentication")
public class ArtifactoryAuthenticationDTO {
  @ApiModelProperty(allowableValues = ArtifactoryConstants.USERNAME_PASSWORD)
  @NotNull
  @JsonProperty("type")
  ArtifactoryAuthType authType;
  @JsonProperty("spec")
  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
  @JsonSubTypes({
    @JsonSubTypes.Type(value = ArtifactoryUsernamePasswordAuthDTO.class, name = ArtifactoryConstants.USERNAME_PASSWORD)
  })
  @NotNull
  @Valid
  ArtifactoryAuthCredentialsDTO credentials;
}
