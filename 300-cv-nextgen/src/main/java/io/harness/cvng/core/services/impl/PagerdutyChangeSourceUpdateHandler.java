package io.harness.cvng.core.services.impl;

import io.harness.cvng.core.beans.params.ServiceEnvironmentParams;
import io.harness.cvng.core.entities.changeSource.PagerDutyChangeSource;
import io.harness.cvng.core.services.api.PagerDutyService;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PagerdutyChangeSourceUpdateHandler extends ChangeSourceUpdateHandler<PagerDutyChangeSource> {
  @Inject private PagerDutyService pagerDutyService;

  @Override
  public void handleCreate(PagerDutyChangeSource changeSource) {
    if (changeSource.isConfiguredForDemo()) {
      log.info("Not creating pagerduty webhook because change source is configured for demo");
    } else {
      log.info("Creating pagerduty webhook", changeSource);
      ServiceEnvironmentParams serviceEnvironmentParams = ServiceEnvironmentParams.builder()
                                                              .accountIdentifier(changeSource.getAccountId())
                                                              .orgIdentifier(changeSource.getOrgIdentifier())
                                                              .projectIdentifier(changeSource.getProjectIdentifier())
                                                              .serviceIdentifier(changeSource.getServiceIdentifier())
                                                              .environmentIdentifier(changeSource.getEnvIdentifier())
                                                              .build();
      pagerDutyService.registerPagerDutyWebhook(serviceEnvironmentParams, changeSource);
      log.info("Done creating pagerduty webhook", changeSource);
    }
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
