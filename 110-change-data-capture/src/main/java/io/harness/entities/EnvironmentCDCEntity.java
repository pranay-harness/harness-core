package io.harness.entities;

import io.harness.ChangeHandler;
import io.harness.changehandlers.EnvironmentsChangeDataHandler;
import io.harness.ng.core.environment.beans.Environment;
import io.harness.persistence.PersistentEntity;

import com.google.inject.Inject;

public class EnvironmentCDCEntity implements CDCEntity<Environment> {
  @Inject private EnvironmentsChangeDataHandler environmentsChangeDataHandler;

  @Override
  public ChangeHandler getChangeHandler(String handlerClass) {
    return environmentsChangeDataHandler;
  }

  @Override
  public Class<? extends PersistentEntity> getSubscriptionEntity() {
    return Environment.class;
  }
}
