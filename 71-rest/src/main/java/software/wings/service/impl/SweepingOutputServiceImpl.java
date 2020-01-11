package software.wings.service.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.lang.String.format;
import static java.util.Arrays.asList;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.mongodb.DuplicateKeyException;
import io.harness.beans.SweepingOutput;
import io.harness.beans.SweepingOutputInstance;
import io.harness.beans.SweepingOutputInstance.Scope;
import io.harness.beans.SweepingOutputInstance.SweepingOutputInstanceBuilder;
import io.harness.beans.SweepingOutputInstance.SweepingOutputKeys;
import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.CriteriaContainerImpl;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.sweepingoutput.SweepingOutputInquiry;
import software.wings.service.intfc.sweepingoutput.SweepingOutputService;

import javax.validation.executable.ValidateOnExecution;

@ValidateOnExecution
@Singleton
@Slf4j
public class SweepingOutputServiceImpl implements SweepingOutputService {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public SweepingOutputInstance save(SweepingOutputInstance sweepingOutputInstance) {
    try {
      wingsPersistence.save(sweepingOutputInstance);
      return sweepingOutputInstance;
    } catch (DuplicateKeyException exception) {
      throw new InvalidRequestException(
          format("Output with name %s, already saved in the context", sweepingOutputInstance.getName()), exception);
    }
  }

  @Override
  public void ensure(SweepingOutputInstance sweepingOutputInstance) {
    wingsPersistence.saveIgnoringDuplicateKeys(asList(sweepingOutputInstance));
  }

  @Override
  public void copyOutputsForAnotherWorkflowExecution(
      String appId, String fromWorkflowExecutionId, String toWorkflowExecutionId) {
    if (fromWorkflowExecutionId.equals(toWorkflowExecutionId)) {
      return;
    }
    final Query<SweepingOutputInstance> query =
        wingsPersistence.createQuery(SweepingOutputInstance.class)
            .filter(SweepingOutputKeys.appId, appId)
            .filter(SweepingOutputKeys.workflowExecutionIds, fromWorkflowExecutionId);

    UpdateOperations<SweepingOutputInstance> ops =
        wingsPersistence.createUpdateOperations(SweepingOutputInstance.class);
    ops.addToSet(SweepingOutputKeys.workflowExecutionIds, toWorkflowExecutionId);
    wingsPersistence.update(query, ops);
  }

  @Override
  public SweepingOutput findSweepingOutput(SweepingOutputInquiry inquiry) {
    SweepingOutputInstance sweepingOutputInstance = find(inquiry);
    if (sweepingOutputInstance == null) {
      return null;
    }
    return sweepingOutputInstance.getValue();
  }

  @Override
  public SweepingOutputInstance find(SweepingOutputInquiry sweepingOutputInquiry) {
    final Query<SweepingOutputInstance> query = wingsPersistence.createQuery(SweepingOutputInstance.class)
                                                    .filter(SweepingOutputKeys.appId, sweepingOutputInquiry.getAppId())
                                                    .filter(SweepingOutputKeys.name, sweepingOutputInquiry.getName());

    addFilters(sweepingOutputInquiry, query);
    return query.get();
  }

  private void addFilters(SweepingOutputInquiry sweepingOutputInquiry, Query<SweepingOutputInstance> query) {
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
  }

  public static SweepingOutputInstanceBuilder prepareSweepingOutputBuilder(String appId, String pipelineExecutionId,
      String workflowExecutionId, String phaseExecutionId, String stateExecutionId,
      SweepingOutputInstance.Scope sweepingOutputScope) {
    // Default scope is pipeline

    if (pipelineExecutionId == null || Scope.PIPELINE != sweepingOutputScope) {
      pipelineExecutionId = "dummy-" + generateUuid();
    }
    if (workflowExecutionId == null
        || (Scope.PIPELINE != sweepingOutputScope && Scope.WORKFLOW != sweepingOutputScope)) {
      workflowExecutionId = "dummy-" + generateUuid();
    }
    if (phaseExecutionId == null || Scope.STATE == sweepingOutputScope) {
      phaseExecutionId = "dummy-" + generateUuid();
    }
    if (stateExecutionId == null) {
      stateExecutionId = "dummy-" + generateUuid();
    }

    return SweepingOutputInstance.builder()
        .uuid(generateUuid())
        .appId(appId)
        .pipelineExecutionId(pipelineExecutionId)
        .workflowExecutionId(workflowExecutionId)
        .phaseExecutionId(phaseExecutionId)
        .stateExecutionId(stateExecutionId);
  }
}
