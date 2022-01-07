/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.timescale;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.migration.timescale.NGAbstractTimeScaleMigration;

@OwnedBy(DX)
public class CreateInstanceStatsTable extends NGAbstractTimeScaleMigration {
  @Override
  public String getFileName() {
    return "timescale/create_instance_stats_table.sql";
  }
}
