package software.wings.service.impl.apm;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.sm.ExecutionStatus.SUCCESS;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import org.mongodb.morphia.query.Sort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ErrorCode;
import software.wings.beans.WorkflowExecution;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.impl.analysis.SetupTestNodeData;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextFactory;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;

/**
 * Utility files used by Verification services
 * Created by Pranjal on 08/17/2018
 */
public class MLServiceUtil {
  private static final Logger logger = LoggerFactory.getLogger(MLServiceUtil.class);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private ExecutionContextFactory executionContextFactory;

  /**
   * Method to get valid hostname expression.
   * @param nodeData
   * @return hostName
   */
  public String getHostNameFromExpression(final SetupTestNodeData nodeData) {
    WorkflowExecution workflowExecution = wingsPersistence.createQuery(WorkflowExecution.class)
                                              .filter("appId", nodeData.getAppId())
                                              .filter("workflowId", nodeData.getWorkflowId())
                                              .filter("status", SUCCESS)
                                              .order(Sort.descending("createdAt"))
                                              .get();

    if (workflowExecution == null) {
      throw new WingsException(ErrorCode.APM_CONFIGURATION_ERROR)
          .addParam("reason", "No successful execution exists for the workflow.");
    }

    StateExecutionInstance stateExecutionInstance = wingsPersistence.createQuery(StateExecutionInstance.class)
                                                        .filter("executionUuid", workflowExecution.getUuid())
                                                        .filter("stateType", StateType.PHASE)
                                                        .order(Sort.descending("createdAt"))
                                                        .get();
    ExecutionContext executionContext = executionContextFactory.createExecutionContext(stateExecutionInstance, null);
    String hostName = isEmpty(nodeData.getHostExpression())
        ? nodeData.getInstanceName()
        : executionContext.renderExpression(
              nodeData.getHostExpression(), Lists.newArrayList(nodeData.getInstanceElement()));
    logger.info("rendered host is {}", hostName);
    return hostName;
  }
}
