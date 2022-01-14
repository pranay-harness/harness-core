package software.wings.ngmigration;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@OwnedBy(HarnessTeam.CDC)
public class CgEntityNode {
  private String id;
  private NGMigrationEntityType type;
  private NGMigrationEntity entity;
  private CgEntityId entityId;
}
