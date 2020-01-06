package software.wings.beans;

import static io.harness.beans.OrchestrationWorkflowType.BUILD;
import static io.harness.beans.OrchestrationWorkflowType.CANARY;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.EntityType.APPDYNAMICS_APPID;
import static software.wings.beans.EntityType.APPDYNAMICS_CONFIGID;
import static software.wings.beans.EntityType.APPDYNAMICS_TIERID;
import static software.wings.beans.EntityType.CF_AWS_CONFIG_ID;
import static software.wings.beans.EntityType.ELK_CONFIGID;
import static software.wings.beans.EntityType.ELK_INDICES;
import static software.wings.beans.EntityType.ENVIRONMENT;
import static software.wings.beans.EntityType.HELM_GIT_CONFIG_ID;
import static software.wings.beans.EntityType.INFRASTRUCTURE_DEFINITION;
import static software.wings.beans.EntityType.INFRASTRUCTURE_MAPPING;
import static software.wings.beans.EntityType.NEWRELIC_APPID;
import static software.wings.beans.EntityType.NEWRELIC_CONFIGID;
import static software.wings.beans.EntityType.NEWRELIC_MARKER_APPID;
import static software.wings.beans.EntityType.NEWRELIC_MARKER_CONFIGID;
import static software.wings.beans.EntityType.SERVICE;
import static software.wings.beans.EntityType.SPLUNK_CONFIGID;
import static software.wings.beans.EntityType.SUMOLOGIC_CONFIGID;
import static software.wings.beans.Graph.Builder.aGraph;
import static software.wings.beans.GraphLink.Builder.aLink;
import static software.wings.common.Constants.PHASE_NAME_PREFIX;
import static software.wings.common.Constants.WORKFLOW_INFRAMAPPING_VALIDATION_MESSAGE;
import static software.wings.common.Constants.WORKFLOW_VALIDATION_MESSAGE;
import static software.wings.common.Constants.phaseNamePattern;
import static software.wings.service.impl.workflow.WorkflowServiceHelper.ROLLBACK_PREFIX;
import static software.wings.sm.rollback.RollbackStateMachineGenerator.STAGING_PHASE_NAME;
import static software.wings.sm.rollback.RollbackStateMachineGenerator.WHITE_SPACE;

import com.google.common.base.Joiner;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.beans.OrchestrationWorkflowType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.Graph.Builder;
import software.wings.beans.concurrency.ConcurrencyStrategy;
import software.wings.sm.TransitionType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.constraints.NotNull;

@JsonTypeName("CANARY")
@Slf4j
public class CanaryOrchestrationWorkflow extends CustomOrchestrationWorkflow {
  public CanaryOrchestrationWorkflow() {
    setOrchestrationWorkflowType(CANARY);
  }

  private PhaseStep preDeploymentSteps = new PhaseStep(PhaseStepType.PRE_DEPLOYMENT);

  // This is a nullable field
  private PhaseStep rollbackProvisioners;

  @JsonIgnore private List<String> workflowPhaseIds = new ArrayList<>();

  @JsonIgnore private Map<String, WorkflowPhase> workflowPhaseIdMap = new HashMap<>();

  private Map<String, WorkflowPhase> rollbackWorkflowPhaseIdMap = new HashMap<>();

  @Transient private List<WorkflowPhase> workflowPhases = new ArrayList<>();

  private PhaseStep postDeploymentSteps = new PhaseStep(PhaseStepType.POST_DEPLOYMENT);

  private List<NotificationRule> notificationRules = new ArrayList<>();

  private List<FailureStrategy> failureStrategies = new ArrayList<>();

  private List<Variable> systemVariables = new ArrayList<>();

  private List<Variable> userVariables = new ArrayList<>();

  private List<Variable> derivedVariables = new ArrayList<>();

  private Set<EntityType> requiredEntityTypes;

  private static final String WORKFLOW_INFRADEFINITION_VALIDATION_MESSAGE =
      "Some phases %s Infrastructure Definition are found to be invalid/incomplete.";

  @Transient @JsonIgnore private List<String> templateVariables = new ArrayList<>();

  @Override
  public boolean equals(Object o) {
    return super.equals(o) && true;
  }

  public PhaseStep getPreDeploymentSteps() {
    return preDeploymentSteps;
  }

  public void setPreDeploymentSteps(PhaseStep preDeploymentSteps) {
    this.preDeploymentSteps = preDeploymentSteps;
  }

  public PhaseStep getRollbackProvisioners() {
    return rollbackProvisioners;
  }

  public void setRollbackProvisioners(PhaseStep rollbackProvisioners) {
    this.rollbackProvisioners = rollbackProvisioners;
  }

  public List<WorkflowPhase> getWorkflowPhases() {
    return workflowPhases;
  }

  public void setWorkflowPhases(List<WorkflowPhase> workflowPhases) {
    this.workflowPhases = workflowPhases;
  }

  public PhaseStep getPostDeploymentSteps() {
    return postDeploymentSteps;
  }

  public void setPostDeploymentSteps(PhaseStep postDeploymentSteps) {
    this.postDeploymentSteps = postDeploymentSteps;
  }

  @Override
  public List<NotificationRule> getNotificationRules() {
    return notificationRules;
  }

  @Override
  public void setNotificationRules(List<NotificationRule> notificationRules) {
    this.notificationRules = notificationRules;
  }

  public List<FailureStrategy> getFailureStrategies() {
    return failureStrategies;
  }

  public void setFailureStrategies(List<FailureStrategy> failureStrategies) {
    this.failureStrategies = failureStrategies;
  }

  public List<Variable> getSystemVariables() {
    return systemVariables;
  }

  public void setSystemVariables(List<Variable> systemVariables) {
    this.systemVariables = systemVariables;
  }

  @Override
  public List<String> getTemplateVariables() {
    return templateVariables;
  }

  @Override
  public List<Variable> getUserVariables() {
    return userVariables;
  }

  public void setUserVariables(List<Variable> userVariables) {
    this.userVariables = userVariables;
  }

  public List<Variable> getDerivedVariables() {
    return derivedVariables;
  }

  public void setDerivedVariables(List<Variable> derivedVariables) {
    this.derivedVariables = derivedVariables;
  }

  public List<String> getWorkflowPhaseIds() {
    return workflowPhaseIds;
  }

  public void setWorkflowPhaseIds(List<String> workflowPhaseIds) {
    this.workflowPhaseIds = workflowPhaseIds;
  }

  public Map<String, WorkflowPhase> getWorkflowPhaseIdMap() {
    return workflowPhaseIdMap;
  }

  public void setWorkflowPhaseIdMap(Map<String, WorkflowPhase> workflowPhaseIdMap) {
    this.workflowPhaseIdMap = workflowPhaseIdMap;
  }

  public Map<String, WorkflowPhase> getRollbackWorkflowPhaseIdMap() {
    return rollbackWorkflowPhaseIdMap;
  }

  public void setRollbackWorkflowPhaseIdMap(Map<String, WorkflowPhase> rollbackWorkflowPhaseIdMap) {
    this.rollbackWorkflowPhaseIdMap = rollbackWorkflowPhaseIdMap;
  }

  @Override
  public Set<EntityType> getRequiredEntityTypes() {
    return requiredEntityTypes;
  }

  @Override
  public void setRequiredEntityTypes(Set<EntityType> requiredEntityTypes) {
    this.requiredEntityTypes = requiredEntityTypes;
  }

  @Override
  public List<String> getServiceIds() {
    List<WorkflowPhase> phases = new ArrayList<>();
    for (String workflowPhaseId : workflowPhaseIds) {
      WorkflowPhase workflowPhase = workflowPhaseIdMap.get(workflowPhaseId);
      phases.add(workflowPhase);
    }
    return phases.stream()
        .filter(workflowPhase -> workflowPhase.getServiceId() != null)
        .map(WorkflowPhase::getServiceId)
        .distinct()
        .collect(toList());
  }

  @Override
  public List<String> getInfraMappingIds() {
    List<WorkflowPhase> phases = new ArrayList<>();
    for (String workflowPhaseId : workflowPhaseIds) {
      WorkflowPhase workflowPhase = workflowPhaseIdMap.get(workflowPhaseId);
      phases.add(workflowPhase);
    }
    return phases.stream()
        .filter(workflowPhase -> workflowPhase.getInfraMappingId() != null)
        .map(WorkflowPhase::getInfraMappingId)
        .distinct()
        .collect(toList());
  }

  @Override
  public List<String> getInfraDefinitionIds() {
    List<WorkflowPhase> phases = new ArrayList<>();
    for (String workflowPhaseId : workflowPhaseIds) {
      WorkflowPhase workflowPhase = workflowPhaseIdMap.get(workflowPhaseId);
      phases.add(workflowPhase);
    }
    return phases.stream()
        .filter(workflowPhase -> workflowPhase.getInfraDefinitionId() != null)
        .map(WorkflowPhase::getInfraDefinitionId)
        .distinct()
        .collect(toList());
  }

  @Override
  public void setCloneMetadata(Map<String, String> serviceIdMapping) {
    if (workflowPhaseIdMap == null || serviceIdMapping == null) {
      return;
    }
    workflowPhaseIdMap.values().forEach(workflowPhase -> {
      String serviceId = workflowPhase.getServiceId();
      if (serviceId != null) {
        if (serviceIdMapping.containsKey(serviceId)) {
          workflowPhase.setServiceId(serviceIdMapping.get(serviceId));
        }
      }
      workflowPhase.setInfraMappingId(null);
      workflowPhase.setInfraMappingName(null);
    });

    if (rollbackWorkflowPhaseIdMap == null) {
      return;
    }
    rollbackWorkflowPhaseIdMap.values().forEach(rollbackPhase -> {
      String serviceId = rollbackPhase.getServiceId();
      if (serviceId != null) {
        if (serviceIdMapping.containsKey(serviceId)) {
          rollbackPhase.setServiceId(serviceIdMapping.get(serviceId));
        }
      }
      rollbackPhase.setInfraMappingId(null);
      rollbackPhase.setInfraMappingName(null);
    });
  }
  /**
   * Invoked before inserting document in mongo by morphia.
   */
  @Override
  public void onSave() {
    populatePhaseStepIds(preDeploymentSteps);
    if (rollbackProvisioners != null) {
      populatePhaseStepIds(rollbackProvisioners);
    }
    if (workflowPhases != null) {
      workflowPhaseIds = new ArrayList<>();
      workflowPhaseIdMap = new HashMap<>();

      int i = 0;
      for (WorkflowPhase workflowPhase : workflowPhases) {
        ++i;
        if (isBlank(workflowPhase.getName()) || phaseNamePattern.matcher(workflowPhase.getName()).matches()) {
          workflowPhase.setName(PHASE_NAME_PREFIX + i);
        }
        workflowPhaseIds.add(workflowPhase.getUuid());
        workflowPhaseIdMap.put(workflowPhase.getUuid(), workflowPhase);
        populatePhaseStepIds(workflowPhase);

        WorkflowPhase rollbackPhase = rollbackWorkflowPhaseIdMap.get(workflowPhase.getUuid());
        if (rollbackPhase != null) {
          rollbackPhase.setName(ROLLBACK_PREFIX + workflowPhase.getName());
          rollbackPhase.setPhaseNameForRollback(workflowPhase.getName());
          rollbackPhase.setTemplateExpressions(workflowPhase.getTemplateExpressions());
          populatePhaseStepIds(rollbackPhase);
        }
      }
    }
    populatePhaseStepIds(postDeploymentSteps);
    setGraph(generateGraph());
  }

  /**
   * Invoked after loading document from mongo by morphia.
   */
  @Override
  public void onLoad(boolean infraRefactor, Workflow workflow) {
    populatePhaseSteps(preDeploymentSteps, getGraph());
    if (rollbackProvisioners != null) {
      populatePhaseSteps(rollbackProvisioners, getGraph());
    }

    // update related field for envId var.
    if (workflow.checkEnvironmentTemplatized()) {
      updateRelatedFieldEnv(infraRefactor, workflow);
    }

    workflowPhases = new ArrayList<>();
    for (String workflowPhaseId : workflowPhaseIds) {
      WorkflowPhase workflowPhase = workflowPhaseIdMap.get(workflowPhaseId);
      workflowPhases.add(workflowPhase);
      workflowPhase.getPhaseSteps().forEach(phaseStep -> populatePhaseSteps(phaseStep, getGraph()));

      if (infraRefactor) {
        if (workflowPhase.checkInfraDefinitionTemplatized()) {
          updateMetadataInfraDefinition(workflow, workflowPhase);
        }
      } else {
        if (workflowPhase.checkInfraTemplatized()) {
          updateMetadataInfraMapping(workflow, workflowPhase);
        }
      }
    }
    if (rollbackWorkflowPhaseIdMap != null) {
      rollbackWorkflowPhaseIdMap.values().forEach(workflowPhase -> {
        workflowPhase.getPhaseSteps().forEach(phaseStep -> { populatePhaseSteps(phaseStep, getGraph()); });
      });
    }
    populatePhaseSteps(postDeploymentSteps, getGraph());
    if (concurrencyStrategy == null && infraRefactor && !BUILD.equals(getOrchestrationWorkflowType())) {
      setConcurrencyStrategy(ConcurrencyStrategy.builder().build());
    }
    reorderUserVariables();
  }
  private void updateMetadataInfraMapping(Workflow workflow, WorkflowPhase workflowPhase) {
    String infraVarName = workflowPhase.fetchInfraMappingTemplatizedName();
    // if env is not templatised add envId in metadata
    Variable variable = contains(userVariables, infraVarName);
    if (variable != null && variable.getMetadata() != null) {
      if (!workflow.checkEnvironmentTemplatized()) {
        variable.getMetadata().put(Variable.ENV_ID, workflow.getEnvId());
      } else {
        variable.getMetadata().remove(Variable.ENV_ID);
      }
      if (!workflowPhase.checkServiceTemplatized()) {
        variable.getMetadata().put(Variable.SERVICE_ID, workflowPhase.getServiceId());
      } else {
        variable.getMetadata().remove(Variable.SERVICE_ID);
      }
    }
  }

  private void updateMetadataInfraDefinition(Workflow workflow, WorkflowPhase workflowPhase) {
    String infraVarName = workflowPhase.fetchInfraDefinitionTemplatizedName();
    Variable variable = contains(userVariables, infraVarName);
    if (variable != null && variable.getMetadata() != null) {
      if (!workflow.checkEnvironmentTemplatized()) {
        variable.getMetadata().put(Variable.ENV_ID, workflow.getEnvId());
      } else {
        variable.getMetadata().remove(Variable.ENV_ID);
      }
      if (!workflowPhase.checkServiceTemplatized()) {
        variable.getMetadata().put(Variable.SERVICE_ID, workflowPhase.getServiceId());
      } else {
        variable.getMetadata().remove(Variable.SERVICE_ID);
      }
    }
  }

  private void updateRelatedFieldEnv(boolean infraRefactor, Workflow workflow) {
    String envVarName = workflow.fetchEnvTemplatizedName();
    Variable variable = contains(userVariables, envVarName);
    if (variable != null && variable.getMetadata() != null) {
      if (infraRefactor) {
        variable.getMetadata().put(Variable.RELATED_FIELD, Joiner.on(",").join(fetchInfraDefVariableNames()));
      } else {
        variable.getMetadata().put(Variable.RELATED_FIELD, Joiner.on(",").join(fetchInfraMappingVariableNames()));
      }
    }
  }

  private List<String> fetchInfraMappingVariableNames() {
    return userVariables.stream()
        .filter(t -> t.obtainEntityType() != null && t.obtainEntityType().equals(INFRASTRUCTURE_MAPPING))
        .map(Variable::getName)
        .collect(toList());
  }

  private List<String> fetchInfraDefVariableNames() {
    return userVariables.stream()
        .filter(t -> t.obtainEntityType() != null && t.obtainEntityType().equals(INFRASTRUCTURE_DEFINITION))
        .map(Variable::getName)
        .collect(toList());
  }

  /**
   * Re orders the user variables first by Entity - Environment, Service - Service Infra
   */
  private void reorderUserVariables() {
    List<Variable> reorderVariables = new ArrayList<>();
    if (userVariables != null) {
      // First get all Entity type user variables
      List<Variable> entityVariables = getEntityVariables();
      List<Variable> nonEntityVariables = getNonEntityVariables();
      if (entityVariables != null) {
        // Environment, Service and Infra Variables
        addEnvServiceInfraVariables(reorderVariables, entityVariables);

        // Add Cloud formation variables
        addCloudFormationUserVariables(reorderVariables, entityVariables);

        addHelmUserVariables(reorderVariables, entityVariables);

        // AppDynamic state user variables
        addAppDUserVariables(reorderVariables, entityVariables);
        // NewRelic state user variables
        addNewRelicUserVariables(reorderVariables, entityVariables);
        // NewRelic Marker State user variables
        addNewRelicMarkerUserVariables(reorderVariables, entityVariables);
        // Add elk variables
        addElkUserVariables(reorderVariables, entityVariables);
        // Add SUMO variables
        addSumoLogicUserVariables(reorderVariables, entityVariables);
        // Add Splunk variables
        addSplunkUserVariables(reorderVariables, entityVariables);
      }
      if (nonEntityVariables != null) {
        reorderVariables.addAll(nonEntityVariables);
      }
    }
    userVariables = reorderVariables;
  }

  private List<Variable> getNonEntityVariables() {
    return userVariables.stream().filter(variable -> variable.obtainEntityType() == null).collect(toList());
  }

  private void addEnvServiceInfraVariables(List<Variable> reorderVariables, List<Variable> entityVariables) {
    for (Variable variable : entityVariables) {
      EntityType entityType = variable.obtainEntityType();
      if (ENVIRONMENT.equals(entityType)) {
        reorderVariables.add(variable);
        break;
      }
    }
    for (Variable variable : entityVariables) {
      if (SERVICE.equals(variable.obtainEntityType())) {
        if (reorderVariables.stream().noneMatch(variable1 -> variable1.getName().equals(variable.getName()))) {
          reorderVariables.add(variable);
          addRelatedEntity(entityVariables, reorderVariables, variable, INFRASTRUCTURE_MAPPING);
        }
      }
    }
    addRemainingEntity(reorderVariables, entityVariables, INFRASTRUCTURE_MAPPING);
    addRemainingEntity(reorderVariables, entityVariables, INFRASTRUCTURE_DEFINITION);
  }

  private void addAppDUserVariables(List<Variable> reorderVariables, List<Variable> entityVariables) {
    for (Variable variable : entityVariables) {
      if (APPDYNAMICS_CONFIGID.equals(variable.obtainEntityType())) {
        reorderVariables.add(variable);
        entityVariables.stream()
            .filter(var
                -> APPDYNAMICS_APPID.equals(var.obtainEntityType())
                    && var.getName().equals(variable.obtainRelatedField()))
            .findFirst()
            .ifPresent((Variable e) -> {
              reorderVariables.add(e);
              entityVariables.stream()
                  .filter(variable1
                      -> APPDYNAMICS_TIERID.equals(variable1.obtainEntityType())
                          && variable1.getName().equals(e.obtainRelatedField()))
                  .findFirst()
                  .ifPresent(reorderVariables::add);
            });
      }
    }
    for (Variable variable : entityVariables) {
      if (APPDYNAMICS_APPID.equals(variable.obtainEntityType())) {
        if (reorderVariables.stream().noneMatch(variable1 -> variable1.getName().equals(variable.getName()))) {
          reorderVariables.add(variable);
          addRelatedEntity(entityVariables, reorderVariables, variable, APPDYNAMICS_TIERID);
        }
      }
    }
    addRemainingEntity(reorderVariables, entityVariables, APPDYNAMICS_TIERID);
  }

  private void addNewRelicUserVariables(List<Variable> reorderVariables, List<Variable> entityVariables) {
    for (Variable variable : entityVariables) {
      if (NEWRELIC_CONFIGID.equals(variable.obtainEntityType())) {
        reorderVariables.add(variable);
        addRelatedEntity(entityVariables, reorderVariables, variable, NEWRELIC_APPID);
      }
    }
    addRemainingEntity(reorderVariables, entityVariables, NEWRELIC_APPID);
  }

  private void addSplunkUserVariables(List<Variable> reorderVariables, List<Variable> entityVariables) {
    for (Variable variable : entityVariables) {
      if (SPLUNK_CONFIGID.equals(variable.obtainEntityType())) {
        reorderVariables.add(variable);
      }
    }
  }

  private void addNewRelicMarkerUserVariables(List<Variable> reorderVariables, List<Variable> entityVariables) {
    for (Variable variable : entityVariables) {
      if (NEWRELIC_MARKER_CONFIGID.equals(variable.obtainEntityType())) {
        reorderVariables.add(variable);
        addRelatedEntity(entityVariables, reorderVariables, variable, NEWRELIC_MARKER_APPID);
      }
    }
    addRemainingEntity(reorderVariables, entityVariables, NEWRELIC_MARKER_APPID);
  }

  private void addElkUserVariables(List<Variable> reorderVariables, List<Variable> entityVariables) {
    for (Variable variable : entityVariables) {
      if (ELK_CONFIGID.equals(variable.obtainEntityType())) {
        reorderVariables.add(variable);
        addRelatedEntity(entityVariables, reorderVariables, variable, ELK_INDICES);
      }
    }
    addRemainingEntity(reorderVariables, entityVariables, ELK_INDICES);
  }

  private void addSumoLogicUserVariables(List<Variable> reorderVariables, List<Variable> entityVariables) {
    for (Variable variable : entityVariables) {
      if (SUMOLOGIC_CONFIGID.equals(variable.obtainEntityType())) {
        reorderVariables.add(variable);
      }
    }
  }

  private void addCloudFormationUserVariables(List<Variable> reorderVariables, List<Variable> entityVariables) {
    for (Variable variable : entityVariables) {
      if (CF_AWS_CONFIG_ID.equals(variable.obtainEntityType())) {
        reorderVariables.add(variable);
      }
    }
  }

  private void addHelmUserVariables(List<Variable> reorderVariables, List<Variable> entityVariables) {
    for (Variable variable : entityVariables) {
      if (HELM_GIT_CONFIG_ID.equals(variable.obtainEntityType())) {
        reorderVariables.add(variable);
      }
    }
  }

  private void addRemainingEntity(
      List<Variable> reorderVariables, List<Variable> entityVariables, EntityType entityType) {
    for (Variable variable : entityVariables) {
      if (variable.obtainEntityType() != null && variable.obtainEntityType().equals(entityType)) {
        if (reorderVariables.stream().noneMatch(variable1 -> variable1.getName().equals(variable.getName()))) {
          reorderVariables.add(variable);
        }
      }
    }
  }

  private void addRelatedEntity(
      List<Variable> entityVariables, List<Variable> reorderVariables, Variable variable, EntityType entityType) {
    entityVariables.stream()
        .filter(variable1
            -> variable1.obtainEntityType() != null && variable1.obtainEntityType().equals(entityType)
                && variable1.getName().equals(variable.obtainRelatedField()))
        .findFirst()
        .ifPresent(reorderVariables::add);
  }

  @Override
  public void updateUserVariables() {
    List<String> templateVariables = getTemplateVariables();
    List<Variable> newVariables = new ArrayList<>();
    if (userVariables != null) {
      // First get all Entity type user variables
      List<Variable> entityVariables = getEntityVariables();
      List<Variable> nonEntityVariables = getNonEntityVariables();
      if (entityVariables != null) {
        for (Variable variable : entityVariables) {
          if (templateVariables.contains(variable.getName())) {
            newVariables.add(variable);
          }
        }
      }
      newVariables.addAll(nonEntityVariables);
    }
    userVariables = newVariables;
  }

  private Variable contains(List<Variable> variables, String name) {
    return variables.stream().filter(variable -> variable.getName().equals(name)).findFirst().orElse(null);
  }

  private List<Variable> getEntityVariables() {
    return userVariables.stream().filter(variable -> variable.obtainEntityType() != null).collect(toList());
  }

  public void populatePhaseStepIds(WorkflowPhase workflowPhase) {
    if (isEmpty(workflowPhase.getPhaseSteps())) {
      return;
    }
    workflowPhase.getPhaseSteps().forEach(this ::populatePhaseStepIds);
  }

  public void populatePhaseStepIds(PhaseStep phaseStep) {
    if (phaseStep.getSteps() == null) {
      logger.error("Incorrect arguments to populate phaseStepIds: {}", phaseStep);
      return;
    }
    phaseStep.setStepsIds(phaseStep.getSteps().stream().map(GraphNode::getId).collect(toList()));
  }

  private void populatePhaseSteps(PhaseStep phaseStep, Graph graph) {
    if (phaseStep == null || phaseStep.getUuid() == null || graph == null || graph.getSubworkflows() == null
        || graph.getSubworkflows().get(phaseStep.getUuid()) == null) {
      logger.error("Incorrect arguments to populate phaseStep: {}, graph: {}", phaseStep, graph);
      return;
    }
    if (isEmpty(phaseStep.getStepsIds())) {
      //      logger.info("Empty stepList for the phaseStep: {}", phaseStep);
      return;
    }

    Graph subWorkflowGraph = graph.getSubworkflows().get(phaseStep.getUuid());
    if (subWorkflowGraph == null) {
      logger.info("No subworkflow found for the phaseStep: {}", phaseStep);
      return;
    }

    Map<String, GraphNode> nodesMap = subWorkflowGraph.getNodesMap();
    phaseStep.setSteps(phaseStep.getStepsIds().stream().map(nodesMap::get).collect(toList()));
  }

  public Graph generateGraph() {
    String id1 = preDeploymentSteps.getUuid();
    String id2;
    GraphNode preDeploymentNode = preDeploymentSteps.generatePhaseStepNode();
    preDeploymentNode.setOrigin(true);
    Builder graphBuilder =
        aGraph().addNodes(preDeploymentNode).addSubworkflow(id1, preDeploymentSteps.generateSubworkflow(null));

    if (rollbackProvisioners != null) {
      GraphNode rollbackProvisionersNode = rollbackProvisioners.generatePhaseStepNode();
      graphBuilder.addNodes(rollbackProvisionersNode)
          .addSubworkflow(rollbackProvisioners.getUuid(), rollbackProvisioners.generateSubworkflow(null));
    }
    if (workflowPhases != null) {
      for (WorkflowPhase workflowPhase : workflowPhases) {
        id2 = workflowPhase.getUuid();
        graphBuilder.addNodes(workflowPhase.generatePhaseNode())
            .addLinks(aLink()
                          .withId(generateUuid())
                          .withFrom(id1)
                          .withTo(id2)
                          .withType(TransitionType.SUCCESS.name())
                          .build())
            .addSubworkflows(workflowPhase.generateSubworkflows());

        if (rollbackWorkflowPhaseIdMap != null && rollbackWorkflowPhaseIdMap.get(workflowPhase.getUuid()) != null) {
          GraphNode rollbackNode = rollbackWorkflowPhaseIdMap.get(workflowPhase.getUuid()).generatePhaseNode();
          graphBuilder.addNodes(rollbackNode)
              .addSubworkflows(rollbackWorkflowPhaseIdMap.get(workflowPhase.getUuid()).generateSubworkflows());
        }
        id1 = id2;
      }
    }
    id2 = postDeploymentSteps.getUuid();
    graphBuilder.addNodes(postDeploymentSteps.generatePhaseStepNode())
        .addLinks(
            aLink().withId(generateUuid()).withFrom(id1).withTo(id2).withType(TransitionType.SUCCESS.name()).build())
        .addSubworkflow(id2, postDeploymentSteps.generateSubworkflow(null));

    return graphBuilder.build();
  }

  private Graph generateRollbackProvisionersSubworkflow(PhaseStep preDeploymentSteps) {
    return null;
  }

  @Override
  public boolean validate() {
    setValid(true);
    setValidationMessage(null);
    if (workflowPhases != null) {
      validateInternal();
      validateInframapping();
    }
    return isValid();
  }

  @Override
  public boolean validate(boolean infraRefactor) {
    setValid(true);
    setValidationMessage(null);
    if (workflowPhases != null) {
      validateInternal();
      if (infraRefactor) {
        validateDefinitions();
      } else {
        validateInframapping();
      }
    }
    return isValid();
  }

  private void validateInternal() {
    String invalid = "";
    List<String> invalidChildren = workflowPhases.stream()
                                       .filter(workflowPhase -> !workflowPhase.validate())
                                       .map(WorkflowPhase::getName)
                                       .collect(toList());
    if (isNotEmpty(invalidChildren)) {
      setValid(false);
      invalid += invalidChildren.toString();
    }

    if (preDeploymentSteps != null && preDeploymentSteps.getSteps() != null) {
      List<String> invalidChildrenPreDeployment = (preDeploymentSteps.getSteps().stream())
                                                      .filter(preDeploymentStep -> !preDeploymentStep.validate())
                                                      .map(GraphNode::getName)
                                                      .collect(toList());
      if (isNotEmpty(invalidChildrenPreDeployment)) {
        setValid(false);
        invalid += invalidChildrenPreDeployment.toString();
      }
    }

    if (postDeploymentSteps != null && postDeploymentSteps.getSteps() != null) {
      List<String> invalidChildrenPostDeployment = (postDeploymentSteps.getSteps().stream())
                                                       .filter(postDeploymentStep -> !postDeploymentStep.validate())
                                                       .map(GraphNode::getName)
                                                       .collect(toList());
      if (isNotEmpty(invalidChildrenPostDeployment)) {
        setValid(false);
        invalid += invalidChildrenPostDeployment.toString();
      }
    }

    if (rollbackWorkflowPhaseIdMap != null) {
      List<String> invalidRollbacks = new ArrayList<>();
      for (WorkflowPhase workflowPhase : workflowPhases) {
        WorkflowPhase rollbackPhase = rollbackWorkflowPhaseIdMap.get(workflowPhase.getUuid());
        if (rollbackPhase != null) {
          if (!rollbackPhase.validate()) {
            invalidRollbacks.add(rollbackPhase.getName());
          }
        }
      }
      if (!invalidRollbacks.isEmpty()) {
        setValid(false);
        if (!invalid.isEmpty()) {
          invalid += ", ";
        }
        invalid += invalidRollbacks.toString();
      }
    }
    if (!invalid.isEmpty()) {
      setValidationMessage(format(WORKFLOW_VALIDATION_MESSAGE, invalid));
    }
  }

  private void validateInframapping() {
    if (workflowPhases != null) {
      List<String> invalidInfraPhaseIds = new ArrayList<>();
      for (WorkflowPhase phase : workflowPhases) {
        if (phase == null || phase.checkInfraTemplatized()) {
          continue;
        }
        if (isEmpty(phase.getInfraMappingId())) {
          invalidInfraPhaseIds.add(phase.getName());
        }
      }
      if (isNotEmpty(invalidInfraPhaseIds)) {
        setValid(false);
        setValidationMessage(format(WORKFLOW_INFRAMAPPING_VALIDATION_MESSAGE, invalidInfraPhaseIds.toString()));
      }
    }
  }

  private void validateDefinitions() {
    if (workflowPhases != null) {
      List<String> invalidInfraPhaseIds = new ArrayList<>();
      for (WorkflowPhase phase : workflowPhases) {
        if (phase == null || phase.checkInfraDefinitionTemplatized()) {
          continue;
        }
        if (isEmpty(phase.getInfraDefinitionId())) {
          invalidInfraPhaseIds.add(phase.getName());
        }
      }
      if (isNotEmpty(invalidInfraPhaseIds)) {
        setValid(false);
        setValidationMessage(format(WORKFLOW_INFRADEFINITION_VALIDATION_MESSAGE, invalidInfraPhaseIds.toString()));
      }
    }
  }

  public boolean serviceRepeat(WorkflowPhase workflowPhase, boolean infraRefactor) {
    if (workflowPhaseIds == null) {
      return false;
    }
    for (String phaseId : workflowPhaseIds) {
      WorkflowPhase existingPhase = workflowPhaseIdMap.get(phaseId);
      if (infraRefactor) {
        if (existingPhase.getServiceId().equals(workflowPhase.getServiceId())
            && existingPhase.getDeploymentType() == workflowPhase.getDeploymentType()
            && existingPhase.getInfraDefinitionId() != null
            && existingPhase.getInfraDefinitionId().equals(workflowPhase.getInfraDefinitionId())) {
          return true;
        }
      } else {
        if (existingPhase.getServiceId().equals(workflowPhase.getServiceId())
            && existingPhase.getDeploymentType() == workflowPhase.getDeploymentType()
            && existingPhase.getInfraMappingId() != null
            && existingPhase.getInfraMappingId().equals(workflowPhase.getInfraMappingId())) {
          return true;
        }
      }
    }
    return false;
  }

  public boolean containsPhaseWithName(String phaseName) {
    if (workflowPhaseIds == null) {
      return false;
    }
    for (String phaseId : workflowPhaseIds) {
      WorkflowPhase existingPhase = workflowPhaseIdMap.get(phaseId);
      if (StringUtils.equals(phaseName, existingPhase.getName())) {
        return true;
      }
    }
    return false;
  }

  @Override
  public OrchestrationWorkflow cloneInternal() {
    return aCanaryOrchestrationWorkflow()
        .withGraph(getGraph())
        .withPreDeploymentSteps(getPreDeploymentSteps())
        .withWorkflowPhases(getWorkflowPhases())
        .withWorkflowPhaseIds(getWorkflowPhaseIds())
        .withWorkflowPhaseIdMap(getWorkflowPhaseIdMap())
        .withPostDeploymentSteps(getPostDeploymentSteps())
        .withRollbackWorkflowPhaseIdMap(getRollbackWorkflowPhaseIdMap())
        .withNotificationRules(getNotificationRules())
        .withFailureStrategies(getFailureStrategies())
        .withSystemVariables(getSystemVariables())
        .withUserVariables(getUserVariables())
        .withDerivedVariables(getDerivedVariables())
        .withRequiredEntityTypes(getRequiredEntityTypes())
        .withOrchestrationWorkflowType(getOrchestrationWorkflowType())
        .withConcurrencyStrategy(getConcurrencyStrategy())
        .build();
  }

  @Override
  public List<String> getTemplatizedServiceIds() {
    List<String> templatizedServiceIds = new ArrayList<>();
    if (workflowPhases == null) {
      return templatizedServiceIds;
    }
    for (WorkflowPhase workflowPhase : workflowPhases) {
      List<TemplateExpression> templateExpressions = workflowPhase.getTemplateExpressions();
      if (templateExpressions != null) {
        if (templateExpressions.stream().anyMatch(
                templateExpression -> templateExpression.getFieldName().equals("serviceId"))) {
          if (workflowPhase.getServiceId() != null) {
            templatizedServiceIds.add(workflowPhase.getServiceId());
          }
        }
      }
    }
    return templatizedServiceIds.stream().distinct().collect(toList());
  }

  @Override
  public List<String> getTemplatizedInfraMappingIds() {
    List<String> templatizedInfraMappingIds = new ArrayList<>();
    if (workflowPhases == null) {
      return templatizedInfraMappingIds;
    }
    for (WorkflowPhase workflowPhase : workflowPhases) {
      List<TemplateExpression> templateExpressions = workflowPhase.getTemplateExpressions();
      if (templateExpressions != null) {
        if (templateExpressions.stream().anyMatch(
                templateExpression -> templateExpression.getFieldName().equals("infraMappingId"))) {
          if (workflowPhase.getInfraMappingId() != null) {
            templatizedInfraMappingIds.add(workflowPhase.getInfraMappingId());
          }
        }
      }
    }
    return templatizedInfraMappingIds.stream().distinct().collect(toList());
  }

  @Override
  public List<String> getTemplatizedInfraDefinitionIds() {
    List<String> templatizedInfraDefinitionIds = new ArrayList<>();
    if (workflowPhases == null) {
      return templatizedInfraDefinitionIds;
    }
    for (WorkflowPhase workflowPhase : workflowPhases) {
      List<TemplateExpression> templateExpressions = workflowPhase.getTemplateExpressions();
      if (templateExpressions != null) {
        if (templateExpressions.stream().anyMatch(
                templateExpression -> templateExpression.getFieldName().equals("infraDefinitionId"))) {
          if (workflowPhase.getInfraDefinitionId() != null) {
            templatizedInfraDefinitionIds.add(workflowPhase.getInfraDefinitionId());
          }
        }
      }
    }
    return templatizedInfraDefinitionIds.stream().distinct().collect(toList());
  }

  /**
   * Checks if service templatized or not
   * @return
   */
  @Override
  public boolean isServiceTemplatized() {
    if (workflowPhases != null) {
      for (WorkflowPhase workflowPhase : workflowPhases) {
        if (workflowPhase.checkServiceTemplatized()) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Checks if Inframapping templatized or not
   * @return
   */
  @Override
  public boolean isInfraMappingTemplatized() {
    if (workflowPhases != null) {
      for (WorkflowPhase workflowPhase : workflowPhases) {
        List<TemplateExpression> templateExpressions = workflowPhase.getTemplateExpressions();
        if (templateExpressions != null) {
          if (templateExpressions.stream().anyMatch(
                  templateExpression -> templateExpression.getFieldName().equals("infraMappingId"))) {
            return true;
          }
        }
      }
    }
    return false;
  }

  /**
   * Checks if InfraDefinition templatized or not
   * @return
   */
  @Override
  public boolean isInfraDefinitionTemplatized() {
    if (workflowPhases != null) {
      for (WorkflowPhase workflowPhase : workflowPhases) {
        List<TemplateExpression> templateExpressions = workflowPhase.getTemplateExpressions();
        if (templateExpressions != null) {
          if (templateExpressions.stream().anyMatch(
                  templateExpression -> templateExpression.getFieldName().equals("infraDefinitionId"))) {
            return true;
          }
        }
      }
    }
    return false;
  }

  public boolean checkLastPhaseForOnDemandRollback(@NotNull String phaseName) {
    if (isNotEmpty(workflowPhases)) {
      return phaseName.equals(
          STAGING_PHASE_NAME + WHITE_SPACE + workflowPhases.get(workflowPhases.size() - 1).getName());
    }
    return false;
  }

  public static final class CanaryOrchestrationWorkflowBuilder {
    private Graph graph;
    private PhaseStep preDeploymentSteps = new PhaseStep(PhaseStepType.PRE_DEPLOYMENT);
    private List<String> workflowPhaseIds = new ArrayList<>();
    private Map<String, WorkflowPhase> workflowPhaseIdMap = new HashMap<>();
    private Map<String, WorkflowPhase> rollbackWorkflowPhaseIdMap = new HashMap<>();
    private List<WorkflowPhase> workflowPhases = new ArrayList<>();
    private PhaseStep postDeploymentSteps = new PhaseStep(PhaseStepType.POST_DEPLOYMENT);
    private List<NotificationRule> notificationRules = new ArrayList<>();
    private ConcurrencyStrategy concurrencyStrategy;
    private List<FailureStrategy> failureStrategies = new ArrayList<>();
    private List<Variable> systemVariables = new ArrayList<>();
    private List<Variable> userVariables = new ArrayList<>();
    private List<Variable> derivedVariables = new ArrayList<>();
    private Set<EntityType> requiredEntityTypes;
    private OrchestrationWorkflowType orchestrationWorkflowType = CANARY;

    private CanaryOrchestrationWorkflowBuilder() {}

    public static CanaryOrchestrationWorkflowBuilder aCanaryOrchestrationWorkflow() {
      return new CanaryOrchestrationWorkflowBuilder();
    }

    public CanaryOrchestrationWorkflowBuilder withGraph(Graph graph) {
      this.graph = graph;
      return this;
    }

    public CanaryOrchestrationWorkflowBuilder withPreDeploymentSteps(PhaseStep preDeploymentSteps) {
      this.preDeploymentSteps = preDeploymentSteps;
      return this;
    }

    public CanaryOrchestrationWorkflowBuilder withWorkflowPhaseIds(List<String> workflowPhaseIds) {
      this.workflowPhaseIds = workflowPhaseIds;
      return this;
    }

    public CanaryOrchestrationWorkflowBuilder withWorkflowPhases(List<WorkflowPhase> workflowPhases) {
      this.workflowPhases = workflowPhases;
      return this;
    }

    public CanaryOrchestrationWorkflowBuilder withWorkflowPhaseIdMap(Map<String, WorkflowPhase> workflowPhaseIdMap) {
      this.workflowPhaseIdMap = workflowPhaseIdMap;
      return this;
    }

    public CanaryOrchestrationWorkflowBuilder withRollbackWorkflowPhaseIdMap(
        Map<String, WorkflowPhase> rollbackWorkflowPhaseIdMap) {
      this.rollbackWorkflowPhaseIdMap = rollbackWorkflowPhaseIdMap;
      return this;
    }

    public CanaryOrchestrationWorkflowBuilder addWorkflowPhase(WorkflowPhase workflowPhase) {
      this.workflowPhases.add(workflowPhase);
      return this;
    }

    public CanaryOrchestrationWorkflowBuilder withPostDeploymentSteps(PhaseStep postDeploymentSteps) {
      this.postDeploymentSteps = postDeploymentSteps;
      return this;
    }

    public CanaryOrchestrationWorkflowBuilder withNotificationRules(List<NotificationRule> notificationRules) {
      this.notificationRules = notificationRules;
      return this;
    }

    public CanaryOrchestrationWorkflowBuilder withConcurrencyStrategy(ConcurrencyStrategy concurrencyStrategy) {
      this.concurrencyStrategy = concurrencyStrategy;
      return this;
    }

    public CanaryOrchestrationWorkflowBuilder withFailureStrategies(List<FailureStrategy> failureStrategies) {
      this.failureStrategies = failureStrategies;
      return this;
    }

    public CanaryOrchestrationWorkflowBuilder withSystemVariables(List<Variable> systemVariables) {
      this.systemVariables = systemVariables;
      return this;
    }

    public CanaryOrchestrationWorkflowBuilder withUserVariables(List<Variable> userVariables) {
      this.userVariables = userVariables;
      return this;
    }

    public CanaryOrchestrationWorkflowBuilder withDerivedVariables(List<Variable> derivedVariables) {
      this.derivedVariables = derivedVariables;
      return this;
    }

    public CanaryOrchestrationWorkflowBuilder withRequiredEntityTypes(Set<EntityType> requiredEntityTypes) {
      this.requiredEntityTypes = requiredEntityTypes;
      return this;
    }

    public CanaryOrchestrationWorkflowBuilder withOrchestrationWorkflowType(
        OrchestrationWorkflowType orchestrationWorkflowType) {
      this.orchestrationWorkflowType = orchestrationWorkflowType;
      return this;
    }

    public CanaryOrchestrationWorkflow build() {
      CanaryOrchestrationWorkflow canaryOrchestrationWorkflow = new CanaryOrchestrationWorkflow();
      canaryOrchestrationWorkflow.setGraph(graph);
      canaryOrchestrationWorkflow.setPreDeploymentSteps(preDeploymentSteps);
      canaryOrchestrationWorkflow.setWorkflowPhaseIds(workflowPhaseIds);
      canaryOrchestrationWorkflow.setWorkflowPhaseIdMap(workflowPhaseIdMap);
      canaryOrchestrationWorkflow.setRollbackWorkflowPhaseIdMap(rollbackWorkflowPhaseIdMap);
      canaryOrchestrationWorkflow.setWorkflowPhases(workflowPhases);
      canaryOrchestrationWorkflow.setPostDeploymentSteps(postDeploymentSteps);
      canaryOrchestrationWorkflow.setNotificationRules(notificationRules);
      canaryOrchestrationWorkflow.setFailureStrategies(failureStrategies);
      canaryOrchestrationWorkflow.setSystemVariables(systemVariables);
      canaryOrchestrationWorkflow.setUserVariables(userVariables);
      canaryOrchestrationWorkflow.setDerivedVariables(derivedVariables);
      canaryOrchestrationWorkflow.setRequiredEntityTypes(requiredEntityTypes);
      canaryOrchestrationWorkflow.setOrchestrationWorkflowType(orchestrationWorkflowType);
      canaryOrchestrationWorkflow.setConcurrencyStrategy(concurrencyStrategy);
      return canaryOrchestrationWorkflow;
    }
  }
}
