package io.harness.delegate.beans.connector.docker;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DockerAuthenticationDTO {
  @NotNull @JsonProperty("type") DockerAuthType authType;

  @Builder
  public DockerAuthenticationDTO(DockerAuthType authType, DockerAuthCredentialsDTO credentials) {
    this.authType = authType;
    this.credentials = credentials;
  }

  @JsonProperty("spec")
  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
  @NotNull
  @Valid
  DockerAuthCredentialsDTO credentials;
}
