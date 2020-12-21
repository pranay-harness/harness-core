package io.harness.delegate.beans.connector.scm.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel("GithubSshCredentials")
public class GithubSshCredentialsDTO implements GithubCredentialsDTO {
  @Valid @NotNull GithubSshCredentialsSpecDTO spec;
}
