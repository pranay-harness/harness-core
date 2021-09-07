package io.harness.migrations.timescaledb;

public class AddStorageSupportK8sUtilTable extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/add_storage_related_k8s_util_table.sql";
  }
}
