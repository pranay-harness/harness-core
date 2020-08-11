package io.harness.artifacts.docker.beans;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

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

  public boolean hasCredentials() {
    return isNotEmpty(username);
  }

  public String getDockerRegistryUrl() {
    return dockerRegistryUrl.endsWith("/") ? dockerRegistryUrl : dockerRegistryUrl.concat("/");
  }
}
