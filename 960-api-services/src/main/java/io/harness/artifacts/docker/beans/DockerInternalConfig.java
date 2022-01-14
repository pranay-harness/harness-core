package io.harness.artifacts.docker.beans;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.ToString;
import lombok.Value;

@Value
@Builder
@ToString(exclude = "password")
@OwnedBy(HarnessTeam.CDC)
public class DockerInternalConfig {
  String dockerRegistryUrl;
  String username;
  String password;
  boolean isCertValidationRequired;

  public boolean hasCredentials() {
    return isNotEmpty(username);
  }

  public String getDockerRegistryUrl() {
    return dockerRegistryUrl.endsWith("/") ? dockerRegistryUrl : dockerRegistryUrl.concat("/");
  }
}
