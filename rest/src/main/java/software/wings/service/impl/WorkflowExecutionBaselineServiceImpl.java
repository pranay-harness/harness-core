package software.wings.service.impl;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.beans.SearchFilter.Operator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.PipelineExecution;
import software.wings.beans.PipelineStageExecution;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowType;
import software.wings.beans.baseline.WorkflowExecutionBaseline;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.WorkflowExecutionBaselineService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by rsingh on 2/16/18.
 */
public class WorkflowExecutionBaselineServiceImpl implements WorkflowExecutionBaselineService {
  private static final Logger logger = LoggerFactory.getLogger(WorkflowExecutionBaselineServiceImpl.class);

  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void markBaseline(
      List<WorkflowExecutionBaseline> workflowExecutionBaselines, String executionId, boolean isBaseline) {
    Preconditions.checkState(!isEmpty(workflowExecutionBaselines));

    for (WorkflowExecutionBaseline workflowExecutionBaseline : workflowExecutionBaselines) {
      List<WorkflowExecutionBaseline> existingBaselines =
          wingsPersistence.createQuery(WorkflowExecutionBaseline.class)
              .filter(WorkflowExecutionBaseline.APP_ID_KEY, workflowExecutionBaseline.getAppId())
              .filter("workflowId", workflowExecutionBaseline.getWorkflowId())
              .filter("envId", workflowExecutionBaseline.getEnvId())
              .filter("serviceId", workflowExecutionBaseline.getServiceId())
              .asList();
      if (!isEmpty(existingBaselines)) {
        Preconditions.checkState(
            existingBaselines.size() == 1, "found more than 1 baselines for " + workflowExecutionBaseline);
        WorkflowExecutionBaseline executionBaseline = existingBaselines.get(0);
        String workflowExecutionId = executionBaseline.getWorkflowExecutionId();
        logger.info("marking {} to not to be baseline", workflowExecutionId);
        wingsPersistence.updateField(WorkflowExecution.class, workflowExecutionId, "isBaseline", false);

        // mark workflows in pipeline to be baseline
        markPipelineWorkflowBaselineIfNecessary(executionBaseline, false);
        if (isBaseline) {
          Map<String, Object> updates = new HashMap<>();
          updates.put("workflowExecutionId", workflowExecutionBaseline.getWorkflowExecutionId());
          if (!isEmpty(executionBaseline.getPipelineExecutionId())) {
            if (!isEmpty(workflowExecutionBaseline.getPipelineExecutionId())) {
              updates.put("pipelineExecutionId", workflowExecutionBaseline.getPipelineExecutionId());
            }
            updatePipleLineIfNecessary(executionBaseline.getPipelineExecutionId());
          }
          wingsPersistence.updateFields(WorkflowExecutionBaseline.class, executionBaseline.getUuid(), updates);
        } else {
          wingsPersistence.delete(WorkflowExecutionBaseline.class, executionBaseline.getUuid());
        }
      } else {
        Preconditions.checkState(isBaseline,
            "Unsetting baselines which were not marked as baseline before " + executionId
                + " baselines: " + workflowExecutionBaseline);
        wingsPersistence.save(workflowExecutionBaseline);
      }

      logger.info("marking {} to be baseline", workflowExecutionBaseline);
      wingsPersistence.updateField(
          WorkflowExecution.class, workflowExecutionBaseline.getWorkflowExecutionId(), "isBaseline", isBaseline);
      markPipelineWorkflowBaselineIfNecessary(workflowExecutionBaseline, isBaseline);
    }

    updatePipleLineIfNecessary(executionId);
  }

  private void markPipelineWorkflowBaselineIfNecessary(WorkflowExecutionBaseline baseline, boolean isBaseline) {
    WorkflowExecution execution =
        wingsPersistence.get(WorkflowExecution.class, baseline.getAppId(), baseline.getWorkflowExecutionId());
    if (isEmpty(execution.getPipelineExecutionId())) {
      return;
    }

    WorkflowExecution workflowExecution = isEmpty(baseline.getPipelineExecutionId())
        ? wingsPersistence.get(WorkflowExecution.class, baseline.getAppId(), execution.getPipelineExecutionId())
        : wingsPersistence.get(WorkflowExecution.class, baseline.getAppId(), baseline.getPipelineExecutionId());
    if (workflowExecution == null) {
      return;
    }

    PipelineExecution pipelineExecution = workflowExecution.getPipelineExecution();
    if (pipelineExecution == null) {
      return;
    }

    List<PipelineStageExecution> pipelineStageExecutions = pipelineExecution.getPipelineStageExecutions();
    if (pipelineStageExecutions == null) {
      return;
    }

    pipelineStageExecutions.forEach(
        pipelineStageExecution -> pipelineStageExecution.getWorkflowExecutions().forEach(pipelineWorkflowExecution -> {
          if (pipelineWorkflowExecution.getUuid().equals(baseline.getWorkflowExecutionId())) {
            pipelineWorkflowExecution.setBaseline(isBaseline);
          }
        }));
    wingsPersistence.save(workflowExecution);
  }

  private void updatePipleLineIfNecessary(String executionId) {
    WorkflowExecution workflowExecution = wingsPersistence.get(WorkflowExecution.class, executionId);
    if (workflowExecution.getWorkflowType() != WorkflowType.PIPELINE) {
      return;
    }
    boolean markBaseline = false;
    PipelineExecution pipelineExecution = workflowExecution.getPipelineExecution();
    if (pipelineExecution == null) {
      return;
    }

    List<PipelineStageExecution> pipelineStageExecutions = pipelineExecution.getPipelineStageExecutions();
    if (pipelineStageExecutions == null) {
      return;
    }

    for (PipelineStageExecution pipelineStageExecution : pipelineStageExecutions) {
      for (WorkflowExecution pipelineWorkflowExecution : pipelineStageExecution.getWorkflowExecutions()) {
        if (pipelineWorkflowExecution.isBaseline()) {
          markBaseline = true;
          break;
        }
      }
      if (markBaseline) {
        break;
      }
    }

    wingsPersistence.updateField(WorkflowExecution.class, executionId, "isBaseline", markBaseline);
  }

  @Override
  public String getBaselineExecutionId(String appId, String workflowId, String envId, String serviceId) {
    PageRequest<WorkflowExecutionBaseline> pageRequest = aPageRequest()
                                                             .addFilter("appId", Operator.EQ, appId)
                                                             .addFilter("workflowId", Operator.EQ, workflowId)
                                                             .addFilter("envId", Operator.EQ, envId)
                                                             .addFilter("serviceId", Operator.EQ, serviceId)
                                                             .withLimit("1")
                                                             .build();
    WorkflowExecutionBaseline executionBaseline = wingsPersistence.get(WorkflowExecutionBaseline.class, pageRequest);
    return executionBaseline == null ? null : executionBaseline.getWorkflowExecutionId();
  }
}
