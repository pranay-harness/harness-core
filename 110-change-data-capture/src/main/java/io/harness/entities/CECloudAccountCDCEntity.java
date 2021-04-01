package io.harness.entities;

import io.harness.ChangeHandler;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.changehandlers.TimeScaleDBChangeHandler;
import io.harness.persistence.PersistentEntity;

import software.wings.beans.ce.CECloudAccount;

import com.google.inject.Inject;

@OwnedBy(HarnessTeam.CE)
public class CECloudAccountCDCEntity implements CDCEntity<CECloudAccount> {
  @Inject TimeScaleDBChangeHandler timeScaleDBChangeHandler;

  @Override
  public ChangeHandler getTimescaleChangeHandler() {
    return timeScaleDBChangeHandler;
  }

  @Override
  public Class<? extends PersistentEntity> getSubscriptionEntity() {
    return CECloudAccount.class;
  }
}
