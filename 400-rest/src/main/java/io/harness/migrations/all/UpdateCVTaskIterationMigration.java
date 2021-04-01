package io.harness.migrations.all;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.migrations.Migration;

import software.wings.dl.WingsPersistence;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.AnalysisContext.AnalysisContextKeys;

import com.google.inject.Inject;

@TargetModule(HarnessModule._390_DB_MIGRATION)
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
