package migrations.timescaledb;

public class CreateKubernetesUtilizationData extends AbstractTimeSaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/create_kubernetes_utilization_data_table.sql";
  }
}
