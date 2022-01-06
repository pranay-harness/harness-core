/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import io.harness.migrations.Migration;

/**
 * Sometimes you add a migration, then revert it.
 * But the `schema` collection value in database would be updated.
 *
 * NoOp migration to handle such cases where `schema` value is ahead of MigrationList value.
 */
public class NoOpMigration implements Migration {
  @Override
  public void migrate() {}
}
