/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.migrations;

/**
 * This migration should be used by classes where we want to add seed data to an installation.
 */

public interface SeedDataMigration {
  void migrate();
}
