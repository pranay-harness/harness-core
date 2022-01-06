/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.migrations.Migration;

import software.wings.beans.SettingAttribute;
import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Pranjal on 08/28/2019
 */
@Slf4j
public class DeleteStaleSlackConfigs implements Migration {
  @Inject WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    log.info("Deleting stale Slack configs");
    wingsPersistence.delete(
        wingsPersistence.createQuery(SettingAttribute.class, excludeAuthority).filter("value.type", "SLACK"));
  }
}
