package io.harness.accesscontrol.migrations;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MigrationConfiguration {
  private boolean enabled;
  private List<MigrationType> migrationTypes;
}
