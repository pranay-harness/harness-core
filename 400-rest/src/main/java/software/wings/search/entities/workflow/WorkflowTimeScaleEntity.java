package software.wings.search.entities.workflow;

import io.harness.persistence.PersistentEntity;

import software.wings.beans.Workflow;
import software.wings.search.framework.ChangeHandler;
import software.wings.search.framework.TimeScaleEntity;
import software.wings.timescale.migrations.MigrateWorkflowsToTimeScaleDB;

import com.google.inject.Inject;
import java.util.Set;

public class WorkflowTimeScaleEntity implements TimeScaleEntity<Workflow> {
  @Inject private WorkflowTimescaleChangeHandler workflowTimescaleChangeHandler;
  @Inject private MigrateWorkflowsToTimeScaleDB migrateWorkflowsToTimeScaleDB;

  public static final Class<Workflow> SOURCE_ENTITY_CLASS = Workflow.class;

  @Override
  public Class<Workflow> getSourceEntityClass() {
    return SOURCE_ENTITY_CLASS;
  }

  @Override
  public ChangeHandler getChangeHandler() {
    return workflowTimescaleChangeHandler;
  }

  @Override
  public boolean toProcessChangeEvent(Set<String> accountIds, PersistentEntity entity) {
    Workflow workflow = (Workflow) entity;

    return accountIds.contains(workflow.getAccountId());
  }

  @Override
  public boolean runMigration(String accountId) {
    return migrateWorkflowsToTimeScaleDB.runTimeScaleMigration(accountId);
  }
}
