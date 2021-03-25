package io.harness.artifacts.docker.beans;

import static io.harness.data.structure.HasPredicate.hasSome;

import lombok.Builder;
import lombok.ToString;
import lombok.Value;

@Value
@Builder
@ToString(exclude = "password")
public class DockerInternalConfig {
  String dockerRegistryUrl;
  String username;
  String password;
  boolean isCertValidationRequired;

  public boolean hasCredentials() {
    return hasSome(username);
  }

  public String getDockerRegistryUrl() {
    return dockerRegistryUrl.endsWith("/") ? dockerRegistryUrl : dockerRegistryUrl.concat("/");
  }
}
