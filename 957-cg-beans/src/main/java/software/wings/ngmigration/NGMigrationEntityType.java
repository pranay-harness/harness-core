package software.wings.ngmigration;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.CDC)
public enum NGMigrationEntityType {
  WORKFLOW,
  PIPELINE,
  ARTIFACT_STREAM,
  CONNECTOR,
  SERVICE,
  ENVIRONMENT,
  SECRET,
  INFRA
}
