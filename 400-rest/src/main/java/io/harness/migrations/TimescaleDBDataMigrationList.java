package io.harness.migrations;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.migrations.timescaledb.data.MigrateWorkflowsToTimeScaleDB;
import io.harness.migrations.timescaledb.data.SetInstancesDeployedInDeployment;
import io.harness.migrations.timescaledb.data.SetRollbackDurationInDeployment;
import io.harness.migrations.timescaledb.data.UpdateEnvSvcCPInDeployment;

import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.tuple.Pair;

@UtilityClass
@TargetModule(Module._390_DB_MIGRATION)
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
