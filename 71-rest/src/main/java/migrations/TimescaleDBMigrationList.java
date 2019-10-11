package migrations;

import com.google.common.collect.ImmutableList;

import migrations.timescaledb.AddIndexToInstanceV2Migration;
import migrations.timescaledb.AddRollbackToDeployment;
import migrations.timescaledb.AddingToCVDeploymentMetrics;
import migrations.timescaledb.ChangeToTimeStampTZ;
import migrations.timescaledb.CreateNewInstanceV2Migration;
import migrations.timescaledb.DeploymentAdditionalColumns;
import migrations.timescaledb.InitSchemaMigration;
import migrations.timescaledb.InitVerificationSchemaMigration;
import migrations.timescaledb.RenameInstanceMigration;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

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
        .build();
  }
}
