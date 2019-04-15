package software.wings.service.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.lang.String.format;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.mongodb.DuplicateKeyException;
import io.harness.beans.SweepingOutput;
import io.harness.beans.SweepingOutput.Scope;
import io.harness.beans.SweepingOutput.SweepingOutputBuilder;
import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.CriteriaContainerImpl;
import org.mongodb.morphia.query.Query;
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
  public SweepingOutput find(
      String appId, String name, String pipelineExecutionId, String workflowExecutionId, String phaseExecutionId) {
    final Query<SweepingOutput> query = wingsPersistence.createQuery(SweepingOutput.class)
                                            .filter(SweepingOutput.APP_ID_KEY, appId)
                                            .filter(SweepingOutput.NAME_KEY, name);

    final CriteriaContainerImpl workflowCriteria =
        query.criteria(SweepingOutput.WORKFLOW_EXECUTION_ID_KEY).equal(workflowExecutionId);
    final CriteriaContainerImpl phaseCriteria =
        query.criteria(SweepingOutput.PHASE_EXECUTION_ID_KEY).equal(phaseExecutionId);

    if (pipelineExecutionId != null) {
      final CriteriaContainerImpl pipelineCriteria =
          query.criteria(SweepingOutput.PIPELINE_EXECUTION_ID_KEY).equal(pipelineExecutionId);
      query.or(pipelineCriteria, workflowCriteria, phaseCriteria);
    } else {
      query.or(workflowCriteria, phaseCriteria);
    }

    return query.get();
  }

  public static SweepingOutputBuilder prepareSweepingOutputBuilder(String appId, String pipelineExecutionId,
      String workflowExecutionId, String phaseExecutionId, SweepingOutput.Scope sweepingOutputScope) {
    // Default scope is pipeline

    if (pipelineExecutionId == null || !Scope.PIPELINE.equals(sweepingOutputScope)) {
      pipelineExecutionId = "dummy-" + generateUuid();
    }
    if (workflowExecutionId == null || Scope.PHASE.equals(sweepingOutputScope)) {
      workflowExecutionId = "dummy-" + generateUuid();
    }
    if (phaseExecutionId == null) {
      phaseExecutionId = "dummy-" + generateUuid();
    }
    return SweepingOutput.builder()
        .appId(appId)
        .pipelineExecutionId(pipelineExecutionId)
        .workflowExecutionId(workflowExecutionId)
        .phaseExecutionId(phaseExecutionId);
  }
}
