package software.wings.sm.states;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import org.mongodb.morphia.annotations.Transient;
import software.wings.api.CanaryWorkflowStandardParams;
import software.wings.api.InstanceElement;
import software.wings.api.PhaseElement;
import software.wings.beans.ElementExecutionSummary;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.SortOrder.OrderType;
import software.wings.beans.WorkflowExecution;
import software.wings.common.Constants;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.State;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by rsingh on 7/6/17.
 */
public abstract class AbstractAnalysisState extends State {
  @Transient @Inject private WorkflowExecutionService workflowExecutionService;

  /**
   * Instantiates a new state.
   *
   * @param name      the name
   * @param stateType the state type
   */
  public AbstractAnalysisState(String name, String stateType) {
    super(name, stateType);
  }

  protected Set<String> getLastExecutionNodes(ExecutionContext context) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    String serviceId = phaseElement.getServiceElement().getUuid();

    final WorkflowExecution executionDetails =
        workflowExecutionService.getExecutionDetails(context.getAppId(), context.getWorkflowExecutionId());
    final PageRequest<WorkflowExecution> pageRequest =
        PageRequest.Builder.aPageRequest()
            .addFilter("appId", Operator.EQ, context.getAppId())
            .addFilter("workflowId", Operator.EQ, executionDetails.getWorkflowId())
            .addFilter("_id", Operator.NOT_EQ, context.getWorkflowExecutionId())
            .addOrder("createdAt", OrderType.DESC)
            .withLimit("1")
            .build();

    final PageResponse<WorkflowExecution> workflowExecutions =
        workflowExecutionService.listExecutions(pageRequest, false);
    Preconditions.checkState(workflowExecutions.size() == 1, "Could not get execution details");
    final WorkflowExecution workflowExecution = workflowExecutions.get(0);

    ElementExecutionSummary executionSummary = null;
    for (ElementExecutionSummary summary : workflowExecution.getServiceExecutionSummaries()) {
      if (summary.getContextElement().getUuid().equals(serviceId)) {
        executionSummary = summary;
        break;
      }
    }

    Preconditions.checkNotNull(executionSummary, "could not find the execution summary for current execution");

    Set<String> hosts = new HashSet<>();
    for (InstanceStatusSummary instanceStatusSummary : executionSummary.getInstanceStatusSummaries()) {
      hosts.add(instanceStatusSummary.getInstanceElement().getHostName());
    }

    return hosts;
  }

  protected Set<String> getCanaryNewHostNames(ExecutionContext context) {
    CanaryWorkflowStandardParams canaryWorkflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    final Set<String> rv = new HashSet<>();
    for (InstanceElement instanceElement : canaryWorkflowStandardParams.getInstances()) {
      rv.add(instanceElement.getHostName());
    }

    return rv;
  }
}
