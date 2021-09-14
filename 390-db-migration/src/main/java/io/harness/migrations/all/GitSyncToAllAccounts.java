/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
