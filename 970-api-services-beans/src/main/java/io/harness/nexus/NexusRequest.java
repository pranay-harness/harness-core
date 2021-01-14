package io.harness.nexus;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NexusRequest {
  String nexusUrl;
  String version = "2.x";
  String username;
  char[] password;
  boolean hasCredentials;
  boolean isCertValidationRequired;
}
