package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.baseline.WorkflowExecutionBaseline;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.WorkflowExecutionBaselineService;

import java.util.List;

/**
 * Created by rsingh on 2/16/18.
 */
public class WorkflowExecutionBaselineServiceImpl implements WorkflowExecutionBaselineService {
  private static final Logger logger = LoggerFactory.getLogger(WorkflowExecutionBaselineServiceImpl.class);

  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void markBaseline(List<WorkflowExecutionBaseline> workflowExecutionBaselines) {
    if (isEmpty(workflowExecutionBaselines)) {
      return;
    }

    for (WorkflowExecutionBaseline workflowExecutionBaseline : workflowExecutionBaselines) {
      List<WorkflowExecutionBaseline> existingBaselines = wingsPersistence.createQuery(WorkflowExecutionBaseline.class)
                                                              .field("workflowId")
                                                              .equal(workflowExecutionBaseline.getWorkflowId())
                                                              .field("envId")
                                                              .equal(workflowExecutionBaseline.getEnvId())
                                                              .field("serviceId")
                                                              .equal(workflowExecutionBaseline.getServiceId())
                                                              .asList();
      if (!isEmpty(existingBaselines)) {
        Preconditions.checkState(
            existingBaselines.size() == 1, "found more than 1 baselines for " + workflowExecutionBaseline);
        WorkflowExecutionBaseline executionBaseline = existingBaselines.get(0);
        String workflowExecutionId = executionBaseline.getWorkflowExecutionId();
        logger.info("marking {} to not to be baseline", workflowExecutionId);
        wingsPersistence.updateField(WorkflowExecution.class, workflowExecutionId, "isBaseline", false);
        wingsPersistence.updateField(WorkflowExecutionBaseline.class, executionBaseline.getUuid(),
            "workflowExecutionId", workflowExecutionBaseline.getWorkflowExecutionId());
      } else {
        wingsPersistence.save(workflowExecutionBaseline);
      }

      logger.info("marking {} to be baseline", workflowExecutionBaseline);
      wingsPersistence.updateField(
          WorkflowExecution.class, workflowExecutionBaseline.getWorkflowExecutionId(), "isBaseline", true);
    }
  }
}
