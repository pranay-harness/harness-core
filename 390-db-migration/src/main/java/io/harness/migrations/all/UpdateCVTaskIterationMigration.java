/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.migrations.all;

import io.harness.migrations.Migration;

import software.wings.dl.WingsPersistence;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.AnalysisContext.AnalysisContextKeys;

import com.google.inject.Inject;

public class UpdateCVTaskIterationMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    wingsPersistence.update(
        wingsPersistence.createQuery(AnalysisContext.class).filter(AnalysisContextKeys.cvTaskCreationIteration, null),
        wingsPersistence.createUpdateOperations(AnalysisContext.class)
            .set(AnalysisContextKeys.cvTaskCreationIteration, 0));
  }
}
