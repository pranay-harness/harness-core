package software.wings.service.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.lang.String.format;
import static java.util.Arrays.asList;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.mongodb.DuplicateKeyException;
import io.harness.beans.SweepingOutput;
import io.harness.beans.SweepingOutput.Scope;
import io.harness.beans.SweepingOutput.SweepingOutputBuilder;
import io.harness.beans.SweepingOutput.SweepingOutputKeys;
import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.CriteriaContainerImpl;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.SweepingOutputService;

import javax.validation.executable.ValidateOnExecution;

@ValidateOnExecution
@Singleton
@Slf4j
public class SweepingOutputServiceImpl implements SweepingOutputService {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public SweepingOutput save(SweepingOutput sweepingOutput) {
    try {
      wingsPersistence.save(sweepingOutput);
      return sweepingOutput;
    } catch (DuplicateKeyException exception) {
      throw new InvalidRequestException(
          format("Output with name %s, already saved in the context", sweepingOutput.getName()), exception);
    }
  }

  @Override
  public void ensure(SweepingOutput sweepingOutput) {
    wingsPersistence.saveIgnoringDuplicateKeys(asList(sweepingOutput));
  }

  @Override
  public void copyOutputsForAnotherWorkflowExecution(
      String appId, String fromWorkflowExecutionId, String toWorkflowExecutionId) {
    if (fromWorkflowExecutionId.equals(toWorkflowExecutionId)) {
      return;
    }
    final Query<SweepingOutput> query = wingsPersistence.createQuery(SweepingOutput.class)
                                            .filter(SweepingOutputKeys.appId, appId)
                                            .filter(SweepingOutputKeys.workflowExecutionIds, fromWorkflowExecutionId);

    UpdateOperations<SweepingOutput> ops = wingsPersistence.createUpdateOperations(SweepingOutput.class);
    ops.addToSet(SweepingOutputKeys.workflowExecutionIds, toWorkflowExecutionId);
    wingsPersistence.update(query, ops);
  }

  public SweepingOutput find(SweepingOutputInquiry sweepingOutputInquiry) {
    final Query<SweepingOutput> query = wingsPersistence.createQuery(SweepingOutput.class)
                                            .filter(SweepingOutputKeys.appId, sweepingOutputInquiry.getAppId())
                                            .filter(SweepingOutputKeys.name, sweepingOutputInquiry.getName());

    final CriteriaContainerImpl workflowCriteria =
        query.criteria(SweepingOutputKeys.workflowExecutionIds).equal(sweepingOutputInquiry.getWorkflowExecutionId());
    final CriteriaContainerImpl phaseCriteria =
        query.criteria(SweepingOutputKeys.phaseExecutionId).equal(sweepingOutputInquiry.getPhaseExecutionId());
    final CriteriaContainerImpl stateCriteria =
        query.criteria(SweepingOutputKeys.stateExecutionId).equal(sweepingOutputInquiry.getStateExecutionId());

    if (sweepingOutputInquiry.getPipelineExecutionId() != null) {
      final CriteriaContainerImpl pipelineCriteria =
          query.criteria(SweepingOutputKeys.pipelineExecutionId).equal(sweepingOutputInquiry.getPipelineExecutionId());
      query.or(pipelineCriteria, workflowCriteria, phaseCriteria, stateCriteria);
    } else {
      query.or(workflowCriteria, phaseCriteria, stateCriteria);
    }
    return query.get();
  }

  public static SweepingOutputBuilder prepareSweepingOutputBuilder(String appId, String pipelineExecutionId,
      String workflowExecutionId, String phaseExecutionId, String stateExecutionId,
      SweepingOutput.Scope sweepingOutputScope) {
    // Default scope is pipeline

    if (pipelineExecutionId == null || !Scope.PIPELINE.equals(sweepingOutputScope)) {
      pipelineExecutionId = "dummy-" + generateUuid();
    }
    if (workflowExecutionId == null
        || (!Scope.PIPELINE.equals(sweepingOutputScope) && !Scope.WORKFLOW.equals(sweepingOutputScope))) {
      workflowExecutionId = "dummy-" + generateUuid();
    }
    if (phaseExecutionId == null || Scope.STATE.equals(sweepingOutputScope)) {
      phaseExecutionId = "dummy-" + generateUuid();
    }
    if (stateExecutionId == null) {
      stateExecutionId = "dummy-" + generateUuid();
    }

    return SweepingOutput.builder()
        .uuid(generateUuid())
        .appId(appId)
        .pipelineExecutionId(pipelineExecutionId)
        .workflowExecutionId(workflowExecutionId)
        .phaseExecutionId(phaseExecutionId)
        .stateExecutionId(stateExecutionId);
  }
}
