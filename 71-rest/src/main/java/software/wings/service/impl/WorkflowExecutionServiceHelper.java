package software.wings.service.impl;

import static io.harness.beans.WorkflowType.ORCHESTRATION;
import static io.harness.beans.WorkflowType.PIPELINE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.VariableType.ENTITY;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.ExecutionArgs;
import software.wings.beans.Pipeline;
import software.wings.beans.Variable;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.deployment.WorkflowVariablesMetadata;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class WorkflowExecutionServiceHelper {
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private WorkflowService workflowService;
  @Inject private PipelineService pipelineService;
  private static final String CHANGED_MESSAGE =
      "Workflow Variables have changed since previous execution. Please select new values.";

  public WorkflowVariablesMetadata fetchWorkflowVariables(
      String appId, ExecutionArgs executionArgs, String workflowExecutionId) {
    List<Variable> workflowVariables = fetchWorkflowVariables(appId, executionArgs);
    if (isBlank(workflowExecutionId) || isEmpty(workflowVariables)) {
      return new WorkflowVariablesMetadata(workflowVariables);
    }

    WorkflowExecution workflowExecution = workflowExecutionService.getWorkflowExecution(appId, workflowExecutionId);
    if (workflowExecution == null || workflowExecution.getExecutionArgs() == null
        || !executionArgs.getWorkflowType().equals(workflowExecution.getWorkflowType())
        || (ORCHESTRATION.equals(workflowExecution.getWorkflowType())
               && !executionArgs.getOrchestrationId().equals(workflowExecution.getWorkflowId()))
        || (PIPELINE.equals(workflowExecution.getWorkflowType())
               && (workflowExecution.getPipelineExecution() == null
                      || workflowExecution.getPipelineExecution().getPipeline() == null
                      || !executionArgs.getPipelineId().equals(
                             workflowExecution.getPipelineExecution().getPipelineId())))) {
      return new WorkflowVariablesMetadata(workflowVariables);
    }

    Map<String, String> oldWorkflowVariablesValueMap = workflowExecution.getExecutionArgs().getWorkflowVariables();
    if (isEmpty(oldWorkflowVariablesValueMap)) {
      return new WorkflowVariablesMetadata(workflowVariables);
    }

    boolean changed = populateWorkflowVariablesValues(workflowVariables, new HashMap<>(oldWorkflowVariablesValueMap));
    String changedMessage = null;
    if (changed) {
      changedMessage = CHANGED_MESSAGE;
    }
    return new WorkflowVariablesMetadata(workflowVariables, changed, changedMessage);
  }

  private List<Variable> fetchWorkflowVariables(String appId, ExecutionArgs executionArgs) {
    // Fetch workflow variables without any value.
    List<Variable> workflowVariables;
    if (ORCHESTRATION.equals(executionArgs.getWorkflowType())) {
      Workflow workflow = workflowService.readWorkflowWithoutServices(appId, executionArgs.getOrchestrationId());
      workflowVariables = (workflow == null || workflow.getOrchestrationWorkflow() == null)
          ? null
          : workflow.getOrchestrationWorkflow().getUserVariables();
    } else {
      Pipeline pipeline = pipelineService.readPipelineWithVariables(appId, executionArgs.getPipelineId());
      workflowVariables = (pipeline == null) ? null : pipeline.getPipelineVariables();
    }
    return (workflowVariables == null) ? Collections.emptyList() : workflowVariables;
  }

  /**
   * Populate workflow variables with values based on old workflow variables.
   * Current Behaviour:
   * - Don't set values if variable name not present in oldWorkflowVariablesMap
   * - Don't set values if variable corresponding to name is not of the same type and possibly entity type
   * - Otherwise set values
   * - Finally if value is not found for any entity-type variable or there is an entity-variable not present in current
   *   workflow variables, all entity-type variables are reset. This is to make the behaviour simple and predictable for
   *   now. This can made more intelligent later on.
   *
   * @param workflowVariables       current workflow variables - non-empty
   * @param oldWorkflowVariablesMap workflow variables from an older execution - non-empty
   * @return true if entity-type variables are reset, otherwise false
   */
  private boolean populateWorkflowVariablesValues(
      List<Variable> workflowVariables, Map<String, String> oldWorkflowVariablesMap) {
    // NOTE: workflowVariables and oldWorkflowVariablesMap are not empty.
    // oldEntityVariableNames is a set of all the names of old ENTITY workflow variables.
    boolean resetEntities = false;
    for (Variable variable : workflowVariables) {
      String name = variable.getName();
      boolean isEntity = ENTITY.equals(variable.getType());
      boolean oldVariablePresent = oldWorkflowVariablesMap.containsKey(name);
      if (!oldVariablePresent) {
        // A new workflow variable.
        if (isEntity) {
          // If there is a new workflow variable of type ENTITY set resetEntities = true.
          resetEntities = true;
        }
        continue;
      }
      variable.setValue(oldWorkflowVariablesMap.get(name));
      // This is never a noop as we have already dealt with the case that this workflow variable is new.
      oldWorkflowVariablesMap.remove(name);
    }

    // resetEntities is true if there are one or more new ENTITY workflow variables.
    // oldEntityVariableNames is not empty when one or more ENTITY variables have been removed.
    if (resetEntities || isNotEmpty(oldWorkflowVariablesMap)) {
      resetEntityWorkflowVariables(workflowVariables);
      return true;
    }
    return false;
  }

  private void resetEntityWorkflowVariables(List<Variable> workflowVariables) {
    // NOTE: workflowVariables are not empty.
    for (Variable variable : workflowVariables) {
      if (ENTITY.equals(variable.getType())) {
        variable.setValue(null);
      }
    }
  }
}
