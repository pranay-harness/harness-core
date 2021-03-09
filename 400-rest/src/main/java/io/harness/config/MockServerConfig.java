package io.harness.config;

import io.harness.annotations.dev.OwnedBy;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import static io.harness.annotations.dev.HarnessTeam.CDC;

@OwnedBy(CDC)
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MockServerConfig {
  String baseUrl;
  String port;
}
