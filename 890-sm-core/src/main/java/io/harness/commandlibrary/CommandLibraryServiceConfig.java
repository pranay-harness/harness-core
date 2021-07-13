package io.harness.commandlibrary;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(PL)
public class CommandLibraryServiceConfig {
  String baseUrl;
  String managerToCommandLibraryServiceSecret;
  boolean publishingAllowed;
  String publishingSecret;
}
