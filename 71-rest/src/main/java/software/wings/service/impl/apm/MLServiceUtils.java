package software.wings.service.impl.apm;

import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Sort;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionKeys;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.analysis.SetupTestNodeData;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextFactory;
import software.wings.sm.StateExecutionContext;
import software.wings.sm.StateExecutionContext.StateExecutionContextBuilder;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateExecutionInstance.StateExecutionInstanceKeys;
import software.wings.sm.StateType;

import javax.annotation.Nullable;

/**
 * Utility files used by Verification services
 * Created by Pranjal on 08/17/2018
 */
@Slf4j
public class MLServiceUtils {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ExecutionContextFactory executionContextFactory;

  /**
   * Method to get valid hostname expression.
   * @param nodeData
   * @return hostName
   */
  @Nullable
  public String getHostName(final SetupTestNodeData nodeData) {
    if (nodeData.isServiceLevel()) {
      // // this can return null because service guard does not set it.
      return null;
    }
    if (nodeData.getInstanceElement() == null) {
      // The hostname field is editable so user can enter some value there.
      // Also for the fist time execution
      return nodeData.getInstanceName();
    }
    WorkflowExecution workflowExecution = wingsPersistence.createQuery(WorkflowExecution.class)
                                              .filter("appId", nodeData.getAppId())
                                              .filter(WorkflowExecutionKeys.workflowId, nodeData.getWorkflowId())
                                              .filter(WorkflowExecutionKeys.status, SUCCESS)
                                              .order(Sort.descending(WorkflowExecutionKeys.createdAt))
                                              .get();

    Preconditions.checkNotNull(workflowExecution, "No successful execution exists for the workflow.");
    StateExecutionInstance stateExecutionInstance =
        wingsPersistence.createQuery(StateExecutionInstance.class)
            .filter(StateExecutionInstanceKeys.executionUuid, workflowExecution.getUuid())
            .filter(StateExecutionInstanceKeys.stateType, StateType.PHASE)
            .order(Sort.descending(StateExecutionInstanceKeys.createdAt))
            .get();
    ExecutionContext executionContext = executionContextFactory.createExecutionContext(stateExecutionInstance, null);
    StateExecutionContextBuilder contextBuilder = StateExecutionContext.builder();

    if (nodeData.getInstanceElement().getInstanceDetails() != null) {
      StateExecutionData stateExecutionData = new StateExecutionData();
      stateExecutionData.setTemplateVariable(
          ImmutableMap.<String, Object>builder()
              .put("instanceDetails", nodeData.getInstanceElement().getInstanceDetails())
              .build());
      contextBuilder = contextBuilder.stateExecutionData(stateExecutionData);
    }
    if (nodeData.getInstanceElement().getInstance() != null) {
      contextBuilder = contextBuilder.contextElements(Lists.newArrayList(nodeData.getInstanceElement().getInstance()));
    }

    String hostName = isEmpty(nodeData.getHostExpression())
        ? nodeData.getInstanceName()
        : executionContext.renderExpression(nodeData.getHostExpression(), contextBuilder.build());

    logger.info("rendered host is {}", hostName);
    return hostName;
  }
}
