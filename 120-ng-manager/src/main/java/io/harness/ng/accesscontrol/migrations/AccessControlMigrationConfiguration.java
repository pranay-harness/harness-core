package io.harness.ng.accesscontrol.migrations;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.PL)
public class AccessControlMigrationConfiguration {
  private boolean enabled;
}
