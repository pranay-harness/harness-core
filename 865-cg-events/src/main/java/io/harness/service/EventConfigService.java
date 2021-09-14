/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.service;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.CgEventConfig;

import software.wings.service.intfc.ownership.OwnedByApplication;

import java.util.List;

@OwnedBy(CDC)
public interface EventConfigService extends OwnedByApplication {
  List<CgEventConfig> listAllEventsConfig(String accountId, String appId);

  CgEventConfig getEventsConfig(String accountId, String appId, String eventConfigId);

  CgEventConfig getEventsConfigByName(String accountId, String appId, String eventConfigName);

  CgEventConfig createEventsConfig(String accountId, String appId, CgEventConfig eventConfig);

  CgEventConfig updateEventsConfig(String accountId, String appId, CgEventConfig eventConfig);

  CgEventConfig updateEventsConfigEnable(String accountId, String appId, CgEventConfig eventConfig);

  void deleteEventsConfig(String accountId, String appId, String eventConfigId);
}
