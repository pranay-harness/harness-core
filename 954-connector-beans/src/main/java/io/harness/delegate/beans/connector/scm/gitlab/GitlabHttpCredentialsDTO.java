package io.harness.delegate.beans.connector.scm.gitlab;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModel;
import io.swagger.v3.oas.annotations.media.Schema;
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
@ApiModel("GitlabHttpCredentials")
@Schema(name = "GitlabHttpCredentials",
    description = "This contains details of the Gitlab credentials used via HTTP connections")
public class GitlabHttpCredentialsDTO implements GitlabCredentialsDTO {
  @NotNull GitlabHttpAuthenticationType type;
  @JsonProperty("spec")
  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
  @Valid
  @NotNull
  GitlabHttpCredentialsSpecDTO httpCredentialsSpec;

  @Builder
  public GitlabHttpCredentialsDTO(GitlabHttpAuthenticationType type, GitlabHttpCredentialsSpecDTO httpCredentialsSpec) {
    this.type = type;
    this.httpCredentialsSpec = httpCredentialsSpec;
  }
}
