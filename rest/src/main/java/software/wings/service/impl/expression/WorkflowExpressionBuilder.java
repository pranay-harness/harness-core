package software.wings.service.impl.expression;

import static software.wings.beans.OrchestrationWorkflowType.BASIC;
import static software.wings.beans.OrchestrationWorkflowType.CANARY;
import static software.wings.beans.OrchestrationWorkflowType.MULTI_SERVICE;

import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.BasicOrchestrationWorkflow;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.MultiServiceOrchestrationWorkflow;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.Variable;
import software.wings.beans.Workflow;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.StateType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by sgurubelli on 8/10/17.
 */
public class WorkflowExpressionBuilder extends ExpressionBuilder {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Inject private ServiceExpressionBuilder serviceExpressionBuilder;
  @Inject private WorkflowService workflowService;

  @Override
  public List<String> getExpressions(String appId, String entityId, String serviceId, StateType stateType) {
    if (serviceId == null) {
      return getExpressions(appId, entityId);
    } else if (stateType == null) {
      return getExpressions(appId, entityId, serviceId);
    } else if (stateType != null && serviceId != null) {
      List<String> expressions = new ArrayList<>();
      expressions.addAll(getExpressions(appId, entityId, serviceId));
      expressions.addAll(getStateTypeExpressions(stateType));
      return expressions;
    }
    return Arrays.asList();
  }

  @Override
  public List<String> getExpressions(String appId, String entityId) {
    List<String> expressions = new ArrayList<>();
    expressions.addAll(getStaticExpressions());
    expressions.addAll(getDynamicExpressions(appId, entityId));
    return expressions;
  }

  @Override
  public List<String> getExpressions(String appId, String entityId, String serviceId) {
    List<String> expressions = new ArrayList<>();
    expressions.addAll(getExpressions(appId, entityId));
    expressions.addAll(serviceExpressionBuilder.getDynamicExpressions(appId, serviceId));
    return expressions;
  }

  @Override
  public List<String> getDynamicExpressions(String appId, String entityId) {
    List<Variable> variables = new ArrayList<>();
    try {
      Workflow workflow = workflowService.readWorkflow(appId, entityId);
      OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
      if (orchestrationWorkflow != null) {
        if (orchestrationWorkflow.getOrchestrationWorkflowType().equals(BASIC)) {
          variables = ((BasicOrchestrationWorkflow) orchestrationWorkflow).getUserVariables();
        } else if (orchestrationWorkflow.getOrchestrationWorkflowType().equals(CANARY)) {
          variables = ((CanaryOrchestrationWorkflow) orchestrationWorkflow).getUserVariables();
        } else if (orchestrationWorkflow.getOrchestrationWorkflowType().equals(MULTI_SERVICE)) {
          variables = ((MultiServiceOrchestrationWorkflow) orchestrationWorkflow).getUserVariables();
        }
      }
    } catch (Exception ex) {
      logger.warn("Exception occurred while reading workflow for appId {} and workflowId {}", appId, entityId);
    }

    List<String> expressions = variables.stream()
                                   .filter(variable -> variable.getName() != null)
                                   .map(variable -> "workflow.variables." + variable.getName())
                                   .collect(Collectors.toList());
    return expressions;
  }
}
