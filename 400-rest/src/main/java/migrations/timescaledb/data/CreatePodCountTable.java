package migrations.timescaledb.data;

import migrations.timescaledb.AbstractTimeScaleDBMigration;

public class CreatePodCountTable extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/create_pod_count_table.sql";
  }
}
