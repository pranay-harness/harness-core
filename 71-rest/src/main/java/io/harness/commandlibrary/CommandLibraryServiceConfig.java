package io.harness.commandlibrary;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CommandLibraryServiceConfig {
  String baseUrl;
}
