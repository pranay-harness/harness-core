package io.harness.cvng.core.services.impl;

import io.harness.cvng.core.beans.params.ServiceEnvironmentParams;
import io.harness.cvng.core.entities.changeSource.PagerDutyChangeSource;
import io.harness.cvng.core.services.api.PagerDutyService;

import com.google.inject.Inject;

public class PagerdutyChangeSourceUpdateHandler extends ChangeSourceUpdateHandler<PagerDutyChangeSource> {
  @Inject private PagerDutyService pagerDutyService;

  @Override
  public void handleCreate(PagerDutyChangeSource changeSource) {
    ServiceEnvironmentParams serviceEnvironmentParams = ServiceEnvironmentParams.builder()
                                                            .accountIdentifier(changeSource.getAccountId())
                                                            .orgIdentifier(changeSource.getOrgIdentifier())
                                                            .projectIdentifier(changeSource.getProjectIdentifier())
                                                            .serviceIdentifier(changeSource.getServiceIdentifier())
                                                            .environmentIdentifier(changeSource.getEnvIdentifier())
                                                            .build();
    pagerDutyService.registerPagerDutyWebhook(serviceEnvironmentParams, changeSource);
  }

  @Override
  public void handleDelete(PagerDutyChangeSource changeSource) {
    ServiceEnvironmentParams serviceEnvironmentParams = ServiceEnvironmentParams.builder()
                                                            .accountIdentifier(changeSource.getAccountId())
                                                            .orgIdentifier(changeSource.getOrgIdentifier())
                                                            .projectIdentifier(changeSource.getProjectIdentifier())
                                                            .serviceIdentifier(changeSource.getServiceIdentifier())
                                                            .environmentIdentifier(changeSource.getEnvIdentifier())
                                                            .build();
    pagerDutyService.deletePagerdutyWebhook(serviceEnvironmentParams, changeSource);
  }

  @Override
  public void handleUpdate(PagerDutyChangeSource existingChangeSource, PagerDutyChangeSource newChangeSource) {
    // Do nothing
  }
}
