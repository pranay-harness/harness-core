package io.harness.enforcement.configs;

import lombok.Value;

@Value
public class ClientInfo {
  private String name;
  private String clientConfig;
  private String secretConfig;
}
