/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.intfc.ownership;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDC)
public interface OwnedBySettingAttribute {
  /**
   * Prune if belongs to settingAttribute.
   *
   * @param appId the app id
   * @param settingId the setting id
   */
  void pruneBySettingAttribute(String appId, String settingId);
}
