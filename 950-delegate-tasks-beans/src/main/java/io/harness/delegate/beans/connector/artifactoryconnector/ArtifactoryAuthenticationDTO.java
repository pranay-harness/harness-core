package io.harness.delegate.beans.connector.artifactoryconnector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
@ApiModel("ArtifactoryAuthentication")
public class ArtifactoryAuthenticationDTO {
  @ApiModelProperty(allowableValues = ArtifactoryConstants.USERNAME_PASSWORD)
  @NotNull
  @JsonProperty("type")
  ArtifactoryAuthType authType;

  @JsonProperty("spec")
  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
  @NotNull
  @Valid
  ArtifactoryAuthCredentialsDTO credentials;

  @Builder
  public ArtifactoryAuthenticationDTO(ArtifactoryAuthType authType, ArtifactoryAuthCredentialsDTO credentials) {
    this.authType = authType;
    this.credentials = credentials;
  }
}
