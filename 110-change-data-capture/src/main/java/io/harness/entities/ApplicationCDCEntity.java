package io.harness.entities;

import io.harness.ChangeHandler;
import io.harness.TimeScaleDBChangeHandler;
import io.harness.persistence.PersistentEntity;

import software.wings.beans.Application;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ApplicationCDCEntity implements CDCEntity<Application> {
  @Inject private TimeScaleDBChangeHandler timeScaleDBChangeHandler;

  @Override
  public ChangeHandler getTimescaleChangeHandler() {
    return timeScaleDBChangeHandler;
  }

  @Override
  public Class<? extends PersistentEntity> getSubscriptionEntity() {
    return Application.class;
  }
}
