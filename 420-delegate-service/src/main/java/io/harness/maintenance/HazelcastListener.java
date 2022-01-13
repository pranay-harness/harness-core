/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.maintenance;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Inject;
import com.hazelcast.core.HazelcastInstance;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Slf4j
public class HazelcastListener implements MaintenanceListener {
  @Inject private HazelcastInstance hazelcastInstance;

  @Override
  public void onShutdown() {
    if (hazelcastInstance != null) {
      log.info("Shutdown started. Leaving hazelcast cluster.");
      hazelcastInstance.shutdown();
    }
  }

  @Override
  public void onEnterMaintenance() {
    // do nothing
  }

  @Override
  public void onLeaveMaintenance() {
    // do nothing
  }
}
