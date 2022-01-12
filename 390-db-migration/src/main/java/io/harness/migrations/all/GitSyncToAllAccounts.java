/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import io.harness.migrations.Migration;

/**
 * Migration script to do a git sync to all accounts.
 * This is needed one time since we want to force a git sync due to field changes.
 * Going forward, the migration service runs git sync when there are migrations to be run.
 * @author rktummala on 4/5/18
 */
public class GitSyncToAllAccounts implements Migration {
  @Override
  public void migrate() {
    // do nothing
  }
}
