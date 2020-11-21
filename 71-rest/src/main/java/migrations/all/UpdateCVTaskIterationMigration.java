package migrations.all;

import software.wings.dl.WingsPersistence;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.AnalysisContext.AnalysisContextKeys;

import com.google.inject.Inject;
import migrations.Migration;

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
