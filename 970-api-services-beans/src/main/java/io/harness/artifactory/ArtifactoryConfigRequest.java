package io.harness.artifactory;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ArtifactoryConfigRequest {
  String artifactoryUrl;
  String username;
  char[] password;
  boolean hasCredentials;
}
