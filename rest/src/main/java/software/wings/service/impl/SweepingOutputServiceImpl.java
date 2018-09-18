package software.wings.service.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.SweepingOutput;
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
  public SweepingOutput find(String appId, String name, String pipelineExecutionId, String workflowExecutionId) {
    final Query<SweepingOutput> query = wingsPersistence.createQuery(SweepingOutput.class)
                                            .filter(SweepingOutput.APP_ID_KEY, appId)
                                            .filter(SweepingOutput.NAME_KEY, name);

    if (pipelineExecutionId != null) {
      query.or(query.criteria(SweepingOutput.PIPELINE_EXECUTION_ID_KEY).equal(pipelineExecutionId),
          query.criteria(SweepingOutput.WORKFLOW_EXECUTION_ID_KEY).equal(workflowExecutionId));
    } else {
      query.filter(SweepingOutput.WORKFLOW_EXECUTION_ID_KEY, workflowExecutionId);
    }

    return query.get();
  }
}
