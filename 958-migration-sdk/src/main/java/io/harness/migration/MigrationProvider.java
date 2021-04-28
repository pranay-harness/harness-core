package io.harness.migration;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import java.util.List;

@OwnedBy(DX)
public interface MigrationProvider {
  /**
   * @return a string value denoting the service name to which the migration belongs ex: "pipeline", "cvng" etc.
   */
  String getServiceName();

  /**
   * @return list of all the Migrations for a service
   */
  List<? extends MigrationDetails> getMigrationDetailsList();
}
