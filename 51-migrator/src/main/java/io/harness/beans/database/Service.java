package io.harness.beans.database;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Service {
  private String name;
  private String version;
}
