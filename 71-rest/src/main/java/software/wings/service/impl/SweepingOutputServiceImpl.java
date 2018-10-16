package software.wings.service.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.mongodb.morphia.query.CriteriaContainerImpl;
import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.SweepingOutput;
import software.wings.beans.SweepingOutput.Scope;
import software.wings.beans.SweepingOutput.SweepingOutputBuilder;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.SweepingOutputService;

import javax.validation.executable.ValidateOnExecution;

@ValidateOnExecution
@Singleton
public class SweepingOutputServiceImpl implements SweepingOutputService {
  private static final Logger logger = LoggerFactory.getLogger(SweepingOutputServiceImpl.class);

  @Inject private WingsPersistence wingsPersistence;

  @Override
  public SweepingOutput save(SweepingOutput sweepingOutput) {
    return wingsPersistence.saveAndGet(SweepingOutput.class, sweepingOutput);
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
