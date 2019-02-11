package software.wings.service.impl.expression;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.EntityType.ENVIRONMENT;
import static software.wings.beans.EntityType.SERVICE;
import static software.wings.beans.SubEntityType.NOTIFICATION_GROUP;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.SubEntityType;
import software.wings.beans.Workflow;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.StateType;

import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Created by sgurubelli on 8/10/17.
 */
@Singleton
public class WorkflowExpressionBuilder extends ExpressionBuilder {
  @Inject private ServiceExpressionBuilder serviceExpressionBuilder;
  @Inject private EnvironmentExpressionBuilder environmentExpressionBuilder;
  @Inject private WorkflowService workflowService;

  @Override
  public Set<String> getExpressions(String appId, String entityId, String serviceId, StateType stateType) {
    return getExpressions(appId, entityId, serviceId, stateType, null);
  }

  @Override
  public Set<String> getExpressions(
      String appId, String entityId, String serviceId, StateType stateType, SubEntityType subEntityType) {
    Workflow workflow = workflowService.readWorkflow(appId, entityId);
    SortedSet<String> expressions = new TreeSet<>();
    if (subEntityType == null) {
      expressions = new TreeSet<>(getWorkflowVariableExpressions(workflow));
      if (isNotBlank(serviceId) && !serviceId.equalsIgnoreCase("All")) {
        expressions.addAll(getExpressions(appId, entityId, serviceId));
      } else {
        expressions.addAll(getExpressions(appId, entityId));
        expressions.addAll(serviceExpressionBuilder.getServiceTemplateVariableExpressions(appId, "All", SERVICE));
        if (workflow != null && workflow.getEnvId() != null) {
          expressions.addAll(
              environmentExpressionBuilder.getServiceTemplateVariableExpressions(appId, workflow.getEnvId()));
        }
      }
      if (stateType != null) {
        expressions.addAll(getStateTypeExpressions(stateType));
      }
    } else if (NOTIFICATION_GROUP.equals(subEntityType)) {
      // Return only the service variables and workflow variables
      expressions = new TreeSet<>(getWorkflowVariableExpressions(workflow, false));
      if (isNotBlank(serviceId) && !serviceId.equalsIgnoreCase("All")) {
        expressions.addAll(serviceExpressionBuilder.getDynamicExpressions(appId, serviceId));
        expressions.addAll(
            serviceExpressionBuilder.getServiceTemplateVariableExpressions(appId, serviceId, ENVIRONMENT));
      } else {
        expressions.addAll(serviceExpressionBuilder.getServiceTemplateVariableExpressions(appId, "All", SERVICE));
        if (workflow != null && workflow.getEnvId() != null) {
          expressions.addAll(
              environmentExpressionBuilder.getServiceTemplateVariableExpressions(appId, workflow.getEnvId()));
        }
      }
    }

    return expressions;
  }

  @Override
  public Set<String> getExpressions(String appId, String entityId) {
    SortedSet<String> expressions = new TreeSet<>();
    expressions.addAll(getStaticExpressions());
    expressions.addAll(getDynamicExpressions(appId, entityId));
    return expressions;
  }

  @Override
  public Set<String> getExpressions(String appId, String entityId, String serviceId) {
    SortedSet<String> expressions = new TreeSet<>();
    expressions.addAll(getExpressions(appId, entityId));
    expressions.addAll(serviceExpressionBuilder.getDynamicExpressions(appId, serviceId));
    expressions.addAll(serviceExpressionBuilder.getServiceTemplateVariableExpressions(appId, serviceId, ENVIRONMENT));
    return expressions;
  }

  @Override
  public Set<String> getDynamicExpressions(String appId, String entityId) {
    return new TreeSet<>();
  }

  private Set<String> getWorkflowVariableExpressions(Workflow workflow) {
    return getWorkflowVariableExpressions(workflow, true);
  }

  private Set<String> getWorkflowVariableExpressions(Workflow workflow, boolean includeEntityType) {
    if (workflow == null || workflow.getOrchestrationWorkflow() == null
        || workflow.getOrchestrationWorkflow().getUserVariables() == null) {
      return new TreeSet<>();
    }
    return workflow.getOrchestrationWorkflow()
        .getUserVariables()
        .stream()
        .filter(variable -> variable.getName() != null && (includeEntityType || variable.obtainEntityType() == null))
        .map(variable -> "workflow.variables." + variable.getName())
        .collect(Collectors.toSet());
  }
}
