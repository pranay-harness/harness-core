package io.harness.ng.accesscontrol.migrations;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MigrationConfiguration {
  private boolean enabled;
}
