/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.migrations.all;

public class EntityNameValidationMigration_All_04 extends EntityNameValidationMigration {
  @Override
  protected boolean skipAccount(String accountId) {
    return false;
  }
}
