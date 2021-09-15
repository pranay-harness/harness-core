/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.gitsync.persistance.testing;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.springdata.SpringPersistenceModule;
import io.harness.springdata.SpringPersistenceTestConfig;

// This module is only for test configs
@OwnedBy(HarnessTeam.DX)
public class GitSyncablePersistenceTestModule extends SpringPersistenceModule {
  @Override
  protected Class<?>[] getConfigClasses() {
    return new Class<?>[] {GitSyncablePersistenceTestConfig.class, SpringPersistenceTestConfig.class};
  }
}
