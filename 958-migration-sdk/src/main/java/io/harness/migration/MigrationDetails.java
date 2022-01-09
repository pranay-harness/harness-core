/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migration;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.migration.beans.MigrationType;

import java.util.List;
import org.apache.commons.lang3.tuple.Pair;

@OwnedBy(DX)
public interface MigrationDetails {
  /**
   * @return the name of migration depending on what type of migration it is, from some predefined types in
   *     MigrationType. The returned value is concatenated with the service name so that each service has its own
   * collection for a particular Migration type.
   */

  MigrationType getMigrationTypeName();

  /**
   * Every Migration can be either Background or Foreground.
   * @return true for a Background migration.
   */
  boolean isBackground();

  /**
   * @return a List of Pair which will contain (version:Integer, MigrationClass) for the same type eg: List of all
   *     MongoMigrations for a microservice.
   */
  List<Pair<Integer, Class<? extends NGMigration>>> getMigrations();
}
