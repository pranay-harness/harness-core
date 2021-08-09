package io.harness.nexus;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(HarnessTeam.CDC)
public class NexusRequest {
  String nexusUrl;
  String version;
  String username;
  char[] password;
  boolean hasCredentials;
  boolean isCertValidationRequired;
}
