package io.harness.commandlibrary;

import io.harness.secret.ConfigSecret;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CommandLibraryServiceConfig {
  String baseUrl;
  @ConfigSecret String managerToCommandLibraryServiceSecret;
  boolean publishingAllowed;
  @ConfigSecret String publishingSecret;
}
