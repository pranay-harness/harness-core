package software.wings.search.entities.service;

import io.harness.persistence.PersistentEntity;

import software.wings.beans.Service;
import software.wings.search.framework.ChangeHandler;
import software.wings.search.framework.TimeScaleEntity;
import software.wings.timescale.migrations.MigrateServicesToTimeScaleDB;

import com.google.inject.Inject;
import java.util.Set;

public class ServiceTimeScaleEntity implements TimeScaleEntity<Service> {
  @Inject private ServiceTimescaleChangeHandler serviceTimescaleChangeHandler;
  @Inject private MigrateServicesToTimeScaleDB migrateServicesToTimeScaleDB;

  public static final Class<Service> SOURCE_ENTITY_CLASS = Service.class;

  @Override
  public Class<Service> getSourceEntityClass() {
    return SOURCE_ENTITY_CLASS;
  }

  @Override
  public ChangeHandler getChangeHandler() {
    return serviceTimescaleChangeHandler;
  }

  @Override
  public boolean toProcessChangeEvent(Set<String> accountIds, PersistentEntity entity) {
    Service service = (Service) entity;

    return accountIds.contains(service.getAccountId());
  }

  @Override
  public boolean runMigration(String accountId) {
    return migrateServicesToTimeScaleDB.runTimeScaleMigration(accountId);
  }
}
