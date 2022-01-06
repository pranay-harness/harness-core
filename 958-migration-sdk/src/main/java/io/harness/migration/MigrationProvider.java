/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migration;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.migration.entities.NGSchema;

import java.util.List;

@OwnedBy(DX)
public interface MigrationProvider {
  /**
   * @return a string value denoting the service name to which the migration belongs ex: "pipeline", "cvng" etc.
   */
  String getServiceName();

  /**
   * @return a Entity class that will extend NGSchema class
   */
  Class<? extends NGSchema> getSchemaClass();

  /**
   * @return list of all the Migrations for a service
   */
  List<Class<? extends MigrationDetails>> getMigrationDetailsList();
}
