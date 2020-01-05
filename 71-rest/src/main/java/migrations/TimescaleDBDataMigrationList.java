package migrations;

import com.google.common.collect.ImmutableList;

import lombok.experimental.UtilityClass;
import migrations.timescaledb.data.MigrateWorkflowsToTimeScaleDB;
import migrations.timescaledb.data.SetInstancesDeployedInDeployment;
import migrations.timescaledb.data.SetRollbackDurationInDeployment;
import migrations.timescaledb.data.UpdateEnvSvcCPInDeployment;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

@UtilityClass
public class TimescaleDBDataMigrationList {
  public static List<Pair<Integer, Class<? extends TimeScaleDBDataMigration>>> getMigrations() {
    return new ImmutableList.Builder<Pair<Integer, Class<? extends TimeScaleDBDataMigration>>>()
        .add(Pair.of(1, MigrateWorkflowsToTimeScaleDB.class))
        .add(Pair.of(2, BaseTimeScaleDBDataMigration.class))
        .add(Pair.of(3, SetRollbackDurationInDeployment.class))
        .add(Pair.of(4, SetInstancesDeployedInDeployment.class))
        .add(Pair.of(5, UpdateEnvSvcCPInDeployment.class))
        .build();
  }
}
