package io.harness.migration.beans;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.Microservice;
import io.harness.annotations.dev.OwnedBy;
import io.harness.migration.MigrationProvider;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(DX)
public class NGMigrationConfiguration {
  List<Class<? extends MigrationProvider>> migrationProviderList;
  Microservice microservice;
}
