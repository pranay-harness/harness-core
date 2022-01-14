package software.wings.search.entities.application;

import io.harness.persistence.PersistentEntity;

import software.wings.beans.Application;
import software.wings.search.framework.ChangeHandler;
import software.wings.search.framework.TimeScaleEntity;
import software.wings.timescale.migrations.MigrateApplicationsToTimeScaleDB;

import com.google.inject.Inject;
import java.util.Set;

public class ApplicationTimeScaleEntity implements TimeScaleEntity<Application> {
  @Inject private ApplicationTimescaleChangeHandler applicationTimescaleChangeHandler;
  @Inject private MigrateApplicationsToTimeScaleDB migrateApplicationsToTimeScaleDB;
  public static final Class<Application> SOURCE_ENTITY_CLASS = Application.class;

  @Override
  public Class<Application> getSourceEntityClass() {
    return SOURCE_ENTITY_CLASS;
  }

  @Override
  public ChangeHandler getChangeHandler() {
    return applicationTimescaleChangeHandler;
  }

  @Override
  public boolean toProcessChangeEvent(Set<String> accountIds, PersistentEntity entity) {
    Application application = (Application) entity;
    return accountIds.contains(application.getAccountId());
  }

  @Override
  public boolean runMigration(String accountId) {
    return migrateApplicationsToTimeScaleDB.runTimeScaleMigration(accountId);
  }
}
