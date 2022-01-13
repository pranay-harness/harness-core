/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

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
