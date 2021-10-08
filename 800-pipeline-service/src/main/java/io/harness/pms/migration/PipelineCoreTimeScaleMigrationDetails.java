package io.harness.pms.migration;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.migration.MigrationDetails;
import io.harness.migration.NGMigration;
import io.harness.migration.beans.MigrationType;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;

@OwnedBy(HarnessTeam.PIPELINE)
public class PipelineCoreTimeScaleMigrationDetails implements MigrationDetails {
  @Override
  public MigrationType getMigrationTypeName() {
    return MigrationType.TimeScaleMigration;
  }

  @Override
  public boolean isBackground() {
    return false;
  }

  @Override
  public List<Pair<Integer, Class<? extends NGMigration>>> getMigrations() {
    return new ImmutableList.Builder<Pair<Integer, Class<? extends NGMigration>>>()
        .add(Pair.of(1, CreateTimescaleCDCTablesWhereNotExist.class))
        .add(Pair.of(2, UpdateTimescalePipelineExecutionSummary.class))
        .add(Pair.of(3, UpdateTimescaleCIPipelineExecutionSummary.class))
        .add(Pair.of(4, UpdateTimescaleTablePipelineExecutionSummaryCd.class))
        .add(Pair.of(5, UpdateTimescaleTableCIWithTriggerInfo.class))
        .add(Pair.of(6, UpdateTsCIWithPR.class))
        .build();
  }
}
