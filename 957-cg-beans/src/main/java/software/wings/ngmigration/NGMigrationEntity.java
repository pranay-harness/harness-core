package software.wings.ngmigration;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnore;

@OwnedBy(HarnessTeam.CDC)
public interface NGMigrationEntity {
  NGMigrationEntityType getMigrationEntityType();

  @JsonIgnore
  default String getMigrationEntityName() {
    return "";
  }
}
