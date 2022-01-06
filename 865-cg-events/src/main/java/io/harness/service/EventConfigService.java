/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
