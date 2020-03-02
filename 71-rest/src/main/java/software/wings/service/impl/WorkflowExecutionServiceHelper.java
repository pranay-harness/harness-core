package software.wings.service.impl;

import static io.harness.beans.OrchestrationWorkflowType.BUILD;
import static io.harness.beans.WorkflowType.ORCHESTRATION;
import static io.harness.beans.WorkflowType.PIPELINE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.VariableType.ENTITY;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.OrchestrationWorkflowType;
import io.harness.exception.InvalidRequestException;
import software.wings.api.CanaryWorkflowStandardParams;
import software.wings.api.DeploymentType;
import software.wings.api.WorkflowElement;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.FeatureName;
import software.wings.beans.Service;
import software.wings.beans.Variable;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.deployment.WorkflowVariablesMetadata;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.InfrastructureDefinitionService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.StateMachine;
import software.wings.sm.WorkflowStandardParams;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;

@Singleton
public class WorkflowExecutionServiceHelper {
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private WorkflowService workflowService;
  @Inject private InfrastructureDefinitionService infrastructureDefinitionService;
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject private PipelineService pipelineService;
  @Inject private FeatureFlagService featureFlagService;

  public WorkflowVariablesMetadata fetchWorkflowVariables(
      String appId, ExecutionArgs executionArgs, String workflowExecutionId) {
    List<Variable> workflowVariables = fetchWorkflowVariables(appId, executionArgs);
    if (isBlank(workflowExecutionId) || isEmpty(workflowVariables)) {
      return new WorkflowVariablesMetadata(workflowVariables);
    }

    WorkflowExecution workflowExecution = workflowExecutionService.getWorkflowExecution(appId, workflowExecutionId);
    if (workflowExecution == null || workflowExecution.getExecutionArgs() == null
        || executionArgs.getWorkflowType() != workflowExecution.getWorkflowType()
        || (ORCHESTRATION == workflowExecution.getWorkflowType()
               && !executionArgs.getOrchestrationId().equals(workflowExecution.getWorkflowId()))
        || (PIPELINE == workflowExecution.getWorkflowType()
               && (workflowExecution.getPipelineExecution() == null
                      || workflowExecution.getPipelineExecution().getPipeline() == null
                      || !executionArgs.getPipelineId().equals(
                             workflowExecution.getPipelineExecution().getPipelineId())))) {
      return new WorkflowVariablesMetadata(workflowVariables);
    }

    Map<String, String> oldWorkflowVariablesValueMap = workflowExecution.getExecutionArgs().getWorkflowVariables();
    if (isEmpty(oldWorkflowVariablesValueMap)) {
      return new WorkflowVariablesMetadata(
          workflowVariables, workflowVariables.stream().anyMatch(variable -> ENTITY == variable.getType()));
    }

    boolean changed = populateWorkflowVariablesValues(workflowVariables, new HashMap<>(oldWorkflowVariablesValueMap));
    return new WorkflowVariablesMetadata(workflowVariables, changed);
  }

  @NotNull
  public Workflow obtainWorkflow(@NotNull String appId, @NotNull String workflowId, boolean infraRefactor) {
    Workflow workflow = workflowService.readWorkflow(appId, workflowId);
    notNullCheck("Error reading workflow. Might be deleted", workflow);
    notNullCheck("Error reading workflow. Might be deleted", workflow.getOrchestrationWorkflow());
    if (!workflow.getOrchestrationWorkflow().isValid()) {
      throw new InvalidRequestException("Workflow requested for execution is not valid/complete.");
    }

    return workflow;
  }

  @NotNull
  public WorkflowExecution obtainExecution(Workflow workflow, StateMachine stateMachine, String resolveEnvId,
      String pipelineExecutionId, @NotNull ExecutionArgs executionArgs, boolean infraRefactor) {
    WorkflowExecution workflowExecution = WorkflowExecution.builder().build();
    workflowExecution.setAppId(workflow.getAppId());

    if (resolveEnvId != null) {
      workflowExecution.setEnvId(resolveEnvId);
      workflowExecution.setEnvIds(Collections.singletonList(resolveEnvId));
    }

    workflowExecution.setWorkflowId(workflow.getUuid());
    workflowExecution.setWorkflowIds(asList(workflow.getUuid()));
    workflowExecution.setName(workflow.getName());
    workflowExecution.setWorkflowType(ORCHESTRATION);
    workflowExecution.setOrchestrationType(workflow.getOrchestrationWorkflow().getOrchestrationWorkflowType());
    workflowExecution.setConcurrencyStrategy(workflow.getOrchestrationWorkflow().getConcurrencyStrategy());
    workflowExecution.setStateMachine(stateMachine);
    workflowExecution.setPipelineExecutionId(pipelineExecutionId);
    workflowExecution.setExecutionArgs(executionArgs);
    workflowExecution.setStageName(executionArgs.getStageName());

    Map<String, String> workflowVariables = executionArgs.getWorkflowVariables();
    List<Service> services = workflowService.getResolvedServices(workflow, workflowVariables);
    if (isNotEmpty(services)) {
      workflowExecution.setServiceIds(services.stream().map(Service::getUuid).collect(toList()));
      if (services.size() == 1) {
        Service targetService = services.get(0);
        boolean useSweepingOutput = (targetService.getDeploymentType() == DeploymentType.SSH
                                        || targetService.getDeploymentType() == DeploymentType.WINRM)
            && featureFlagService.isEnabled(FeatureName.SSH_WINRM_SO, workflow.getAccountId());
        workflowExecution.setUseSweepingOutputs(useSweepingOutput);
      }
    }

    if (infraRefactor) {
      workflowExecution.setInfraDefinitionIds(
          workflowService.getResolvedInfraDefinitionIds(workflow, workflowVariables));
      workflowExecution.setCloudProviderIds(infrastructureDefinitionService.fetchCloudProviderIds(
          workflow.getAppId(), workflowExecution.getInfraDefinitionIds()));
    } else {
      workflowExecution.setInfraMappingIds(workflowService.getResolvedInfraMappingIds(workflow, workflowVariables));
      workflowExecution.setCloudProviderIds(infrastructureMappingService.fetchCloudProviderIds(
          workflow.getAppId(), workflowExecution.getInfraMappingIds()));
    }
    return workflowExecution;
  }

  @NotNull
  public WorkflowStandardParams obtainWorkflowStandardParams(
      String appId, String envId, @NotNull ExecutionArgs executionArgs, Workflow workflow) {
    WorkflowStandardParams stdParams;
    if (workflow.getOrchestrationWorkflow().getOrchestrationWorkflowType() == OrchestrationWorkflowType.CANARY
        || workflow.getOrchestrationWorkflow().getOrchestrationWorkflowType() == OrchestrationWorkflowType.BASIC
        || workflow.getOrchestrationWorkflow().getOrchestrationWorkflowType() == OrchestrationWorkflowType.ROLLING
        || workflow.getOrchestrationWorkflow().getOrchestrationWorkflowType() == OrchestrationWorkflowType.MULTI_SERVICE
        || workflow.getOrchestrationWorkflow().getOrchestrationWorkflowType() == BUILD
        || workflow.getOrchestrationWorkflow().getOrchestrationWorkflowType() == OrchestrationWorkflowType.BLUE_GREEN) {
      stdParams = new CanaryWorkflowStandardParams();

      if (workflow.getOrchestrationWorkflow() instanceof CanaryOrchestrationWorkflow) {
        CanaryOrchestrationWorkflow canaryOrchestrationWorkflow =
            (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
        if (canaryOrchestrationWorkflow.getUserVariables() != null) {
          stdParams.setWorkflowElement(WorkflowElement.builder()
                                           .variables(fetchWorkflowVariablesFromOrchestrationWorkflow(
                                               canaryOrchestrationWorkflow, executionArgs))
                                           .build());
        }
      }
    } else {
      stdParams = new WorkflowStandardParams();
    }

    stdParams.setAppId(appId);
    stdParams.setEnvId(envId);
    if (isNotEmpty(executionArgs.getArtifacts())) {
      stdParams.setArtifactIds(executionArgs.getArtifacts().stream().map(Artifact::getUuid).collect(toList()));
    }

    stdParams.setExecutionCredential(executionArgs.getExecutionCredential());

    stdParams.setExcludeHostsWithSameArtifact(executionArgs.isExcludeHostsWithSameArtifact());
    stdParams.setNotifyTriggeredUserOnly(executionArgs.isNotifyTriggeredUserOnly());
    stdParams.setExecutionHosts(executionArgs.getHosts());
    return stdParams;
  }

  private Map<String, Object> fetchWorkflowVariablesFromOrchestrationWorkflow(
      CanaryOrchestrationWorkflow orchestrationWorkflow, ExecutionArgs executionArgs) {
    Map<String, Object> variables = new HashMap<>();
    if (orchestrationWorkflow.getUserVariables() == null) {
      return variables;
    }
    for (Variable variable : orchestrationWorkflow.getUserVariables()) {
      if (variable.isFixed()) {
        setVariables(variable.getName(), variable.getValue(), variables);
        continue;
      }

      // no input from user
      if (executionArgs == null || isEmpty(executionArgs.getWorkflowVariables())
          || isBlank(executionArgs.getWorkflowVariables().get(variable.getName()))) {
        if (variable.isMandatory() && isBlank(variable.getValue())) {
          throw new InvalidRequestException(
              "Workflow variable [" + variable.getName() + "] is mandatory for execution", USER);
        }
        if (isBlank(variable.getValue())) {
          setVariables(variable.getName(), "", variables);
        } else {
          setVariables(variable.getName(), variable.getValue(), variables);
        }
        continue;
      }
      // Verify for allowed values
      if (isNotEmpty(variable.getAllowedValues())) {
        if (isNotEmpty(variable.getValue())) {
          if (!variable.getAllowedList().contains(variable.getValue())) {
            throw new InvalidRequestException("Workflow variable value [" + variable.getValue()
                + " is not in Allowed Values [" + variable.getAllowedList() + "]");
          }
        }
      }
      setVariables(variable.getName(), executionArgs.getWorkflowVariables().get(variable.getName()), variables);
    }
    return variables;
  }

  private void setVariables(String key, Object value, Map<String, Object> variableMap) {
    if (!isNull(key)) {
      variableMap.put(key, value);
    }
  }

  private boolean isNull(String string) {
    return isEmpty(string) || string.equals("null");
  }

  private List<Variable> fetchWorkflowVariables(String appId, ExecutionArgs executionArgs) {
    // Fetch workflow variables without any value.
    List<Variable> workflowVariables;
    if (ORCHESTRATION == executionArgs.getWorkflowType()) {
      Workflow workflow = workflowService.readWorkflowWithoutServices(appId, executionArgs.getOrchestrationId());
      workflowVariables = (workflow == null || workflow.getOrchestrationWorkflow() == null)
          ? null
          : workflow.getOrchestrationWorkflow().getUserVariables();
    } else {
      workflowVariables = pipelineService.getPipelineVariables(appId, executionArgs.getPipelineId());
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
      boolean isEntity = ENTITY == variable.getType();
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
      if (ENTITY == variable.getType()) {
        variable.setValue(null);
      }
    }
  }
}
