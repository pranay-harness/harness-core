/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.licensing.migrations;

import io.harness.licensing.migrations.licenses.ModuleLicenseMigrationDetails;
import io.harness.migration.MigrationDetails;
import io.harness.migration.MigrationProvider;
import io.harness.migration.entities.NGSchema;

import com.google.common.collect.Lists;
import java.util.List;

public class LicenseManagerMigrationProvider implements MigrationProvider {
  @Override
  public String getServiceName() {
    return "licensemanager";
  }

  @Override
  public Class<? extends NGSchema> getSchemaClass() {
    return LicenseManagerSchema.class;
  }

  @Override
  public List<Class<? extends MigrationDetails>> getMigrationDetailsList() {
    return Lists.newArrayList(ModuleLicenseMigrationDetails.class);
  }
}
