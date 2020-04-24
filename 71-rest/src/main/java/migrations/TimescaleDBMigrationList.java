package migrations;

import com.google.common.collect.ImmutableList;

import lombok.experimental.UtilityClass;
import migrations.timescaledb.AddCostEvents;
import migrations.timescaledb.AddDeploymentTagsToDeployment;
import migrations.timescaledb.AddFieldsToServiceGuardStats;
import migrations.timescaledb.AddFieldsToWorkflowCVMetrics;
import migrations.timescaledb.AddIdleUnallocatedColumns;
import migrations.timescaledb.AddIndexToInstanceV2Migration;
import migrations.timescaledb.AddInstancesDeployedToDeployment;
import migrations.timescaledb.AddMaxUtilColumns;
import migrations.timescaledb.AddRollbackToDeployment;
import migrations.timescaledb.AddSchemaForServiceGuardStats;
import migrations.timescaledb.AddSystemCostBillingData;
import migrations.timescaledb.AddingToCVDeploymentMetrics;
import migrations.timescaledb.AlterCEUtilizationDataTables;
import migrations.timescaledb.ChangeToTimeStampTZ;
import migrations.timescaledb.CreateBillingData;
import migrations.timescaledb.CreateKubernetesUtilizationData;
import migrations.timescaledb.CreateNewInstanceV2Migration;
import migrations.timescaledb.CreateUtilizationData;
import migrations.timescaledb.DeploymentAdditionalColumns;
import migrations.timescaledb.InitSchemaMigration;
import migrations.timescaledb.InitVerificationSchemaMigration;
import migrations.timescaledb.RenameInstanceMigration;
import migrations.timescaledb.UniqueIndexCEUtilizationDataTables;
import migrations.timescaledb.UpdateServiceGuardSchema;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

@UtilityClass
public class TimescaleDBMigrationList {
  public static List<Pair<Integer, Class<? extends TimeScaleDBMigration>>> getMigrations() {
    return new ImmutableList.Builder<Pair<Integer, Class<? extends TimeScaleDBMigration>>>()
        .add(Pair.of(1, InitSchemaMigration.class))
        .add(Pair.of(2, InitVerificationSchemaMigration.class))
        .add(Pair.of(3, RenameInstanceMigration.class))
        .add(Pair.of(4, DeploymentAdditionalColumns.class))
        .add(Pair.of(5, ChangeToTimeStampTZ.class))
        .add(Pair.of(6, CreateNewInstanceV2Migration.class))
        .add(Pair.of(7, AddIndexToInstanceV2Migration.class))
        .add(Pair.of(8, AddRollbackToDeployment.class))
        .add(Pair.of(9, AddingToCVDeploymentMetrics.class))
        .add(Pair.of(10, AddSchemaForServiceGuardStats.class))
        .add(Pair.of(11, AddInstancesDeployedToDeployment.class))
        .add(Pair.of(12, UpdateServiceGuardSchema.class))
        .add(Pair.of(13, AddFieldsToWorkflowCVMetrics.class))
        .add(Pair.of(14, AddFieldsToServiceGuardStats.class))
        .add(Pair.of(15, CreateBillingData.class))
        .add(Pair.of(16, CreateKubernetesUtilizationData.class))
        .add(Pair.of(17, CreateUtilizationData.class))
        .add(Pair.of(18, AlterCEUtilizationDataTables.class))
        .add(Pair.of(19, UniqueIndexCEUtilizationDataTables.class))
        .add(Pair.of(20, AddSystemCostBillingData.class))
        .add(Pair.of(21, AddCostEvents.class))
        .add(Pair.of(22, AddDeploymentTagsToDeployment.class))
        .add(Pair.of(23, AddIdleUnallocatedColumns.class))
        .add(Pair.of(24, AddMaxUtilColumns.class))
        .build();
  }
}
