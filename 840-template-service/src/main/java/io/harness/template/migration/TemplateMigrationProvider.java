/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
