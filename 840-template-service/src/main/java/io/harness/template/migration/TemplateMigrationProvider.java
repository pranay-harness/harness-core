/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.template.migration;

import io.harness.ModuleType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.migration.MigrationDetails;
import io.harness.migration.MigrationProvider;
import io.harness.migration.entities.NGSchema;

import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@Singleton
@OwnedBy(HarnessTeam.CDC)
public class TemplateMigrationProvider implements MigrationProvider {
  @Override
  public String getServiceName() {
    return ModuleType.TEMPLATESERVICE.getDisplayName();
  }

  @Override
  public Class<? extends NGSchema> getSchemaClass() {
    return TemplateServiceSchema.class;
  }

  @Override
  public List<Class<? extends MigrationDetails>> getMigrationDetailsList() {
    return new ArrayList<Class<? extends MigrationDetails>>() {
      { add(TemplateServiceMigrationDetails.class); }
    };
  }
}
