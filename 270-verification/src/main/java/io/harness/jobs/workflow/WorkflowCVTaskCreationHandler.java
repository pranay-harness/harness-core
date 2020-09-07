package io.harness.jobs.workflow;

import com.google.inject.Inject;

import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.AnalysisContext.AnalysisContextKeys;
import software.wings.service.intfc.verification.CVTaskService;

public class WorkflowCVTaskCreationHandler implements Handler<AnalysisContext> {
  @Inject CVTaskService cvTaskService;
  @Inject WingsPersistence wingsPersistence;
  @Override
  public void handle(AnalysisContext context) {
    if (context.getDataCollectionInfov2()
        != null) { // This condition should go away once everything is moved to new framework.
      cvTaskService.createCVTasks(context);
    }
    // mark task created to true even if no dataConnectionInfo is present. this is to ensure iterator does not call this
    // again.
    context.setCvTasksCreated(true);
    wingsPersistence.updateField(AnalysisContext.class, context.getUuid(), AnalysisContextKeys.cvTasksCreated, true);
  }
}
