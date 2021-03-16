package io.harness.entities;

import io.harness.ChangeHandler;
import io.harness.ccm.views.entities.CEView;
import io.harness.changehandlers.ViewTimeScaleDBChangeHandler;
import io.harness.persistence.PersistentEntity;

import com.google.inject.Inject;

public class ViewCDCEntity implements CDCEntity<CEView> {
  @Inject private ViewTimeScaleDBChangeHandler viewTimeScaleDBChangeHandler;

  @Override
  public ChangeHandler getTimescaleChangeHandler() {
    return viewTimeScaleDBChangeHandler;
  }

  @Override
  public Class<? extends PersistentEntity> getSubscriptionEntity() {
    return CEView.class;
  }
}
