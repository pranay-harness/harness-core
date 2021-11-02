package software.wings.search.entities.pipeline;

import io.harness.persistence.PersistentEntity;

import software.wings.beans.Pipeline;
import software.wings.search.framework.ChangeHandler;
import software.wings.search.framework.TimeScaleEntity;
import software.wings.timescale.migrations.MigratePipelinesToTimeScaleDB;

import com.google.inject.Inject;
import java.util.Set;

public class PipelineTimeScaleEntity implements TimeScaleEntity<Pipeline> {
  @Inject private PipelineTimescaleChangeDataHandler pipelineTimescaleChangeDataHandler;
  @Inject private MigratePipelinesToTimeScaleDB migratePipelinesToTimeScaleDB;

  public static final Class<Pipeline> SOURCE_ENTITY_CLASS = Pipeline.class;

  @Override
  public Class<Pipeline> getSourceEntityClass() {
    return SOURCE_ENTITY_CLASS;
  }

  @Override
  public ChangeHandler getChangeHandler() {
    return pipelineTimescaleChangeDataHandler;
  }

  @Override
  public boolean toProcessChangeEvent(Set<String> accountIds, PersistentEntity entity) {
    Pipeline pipeline = (Pipeline) entity;

    return accountIds.contains(pipeline.getAccountId());
  }

  @Override
  public boolean runMigration(String accountId) {
    return migratePipelinesToTimeScaleDB.runTimeScaleMigration(accountId);
  }
}
