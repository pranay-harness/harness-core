package io.harness.commandlibrary;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CommandLibraryServiceConfig {
  String baseUrl;
  String managerToCommandLibraryServiceSecret;
  boolean publishingAllowed;
  String publishingSecret;
}
