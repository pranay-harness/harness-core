package software.wings.beans;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang.StringUtils.isBlank;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.EntityType.ENVIRONMENT;
import static software.wings.beans.EntityType.INFRASTRUCTURE_MAPPING;
import static software.wings.beans.EntityType.SERVICE;
import static software.wings.beans.Graph.Builder.aGraph;
import static software.wings.beans.Graph.Link.Builder.aLink;
import static software.wings.beans.OrchestrationWorkflowType.CANARY;
import static software.wings.common.Constants.PHASE_NAME_PREFIX;
import static software.wings.common.Constants.POST_DEPLOYMENT;
import static software.wings.common.Constants.PRE_DEPLOYMENT;
import static software.wings.common.Constants.ROLLBACK_PREFIX;
import static software.wings.common.Constants.WORKFLOW_INFRAMAPPING_VALIDATION_MESSAGE;
import static software.wings.common.Constants.WORKFLOW_VALIDATION_MESSAGE;
import static software.wings.common.Constants.phaseNamePattern;
import static software.wings.common.UUIDGenerator.getUuid;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Graph.Builder;
import software.wings.beans.Graph.Node;
import software.wings.sm.TransitionType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
/**
 * Created by rishi on 12/21/16.
 */
@JsonTypeName("CANARY")
public class CanaryOrchestrationWorkflow extends CustomOrchestrationWorkflow {
  public CanaryOrchestrationWorkflow() {
    setOrchestrationWorkflowType(CANARY);
  }

  private static final Logger logger = LoggerFactory.getLogger(CanaryOrchestrationWorkflow.class);

  @Embedded private PhaseStep preDeploymentSteps = new PhaseStep(PhaseStepType.PRE_DEPLOYMENT, PRE_DEPLOYMENT);

  @JsonIgnore private List<String> workflowPhaseIds = new ArrayList<>();

  @Embedded @JsonIgnore private Map<String, WorkflowPhase> workflowPhaseIdMap = new HashMap<>();

  @Embedded private Map<String, WorkflowPhase> rollbackWorkflowPhaseIdMap = new HashMap<>();

  @Transient private List<WorkflowPhase> workflowPhases = new ArrayList<>();

  @Embedded private PhaseStep postDeploymentSteps = new PhaseStep(PhaseStepType.POST_DEPLOYMENT, POST_DEPLOYMENT);

  @Embedded private List<NotificationRule> notificationRules = new ArrayList<>();

  @Embedded private List<FailureStrategy> failureStrategies = new ArrayList<>();

  private List<Variable> systemVariables = new ArrayList<>();

  private List<Variable> userVariables = new ArrayList<>();

  private List<Variable> derivedVariables = new ArrayList<>();

  private Set<EntityType> requiredEntityTypes;

  @Transient @JsonIgnore private List<String> templateVariables = new ArrayList<>();

  public PhaseStep getPreDeploymentSteps() {
    return preDeploymentSteps;
  }

  public void setPreDeploymentSteps(PhaseStep preDeploymentSteps) {
    this.preDeploymentSteps = preDeploymentSteps;
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
    if (workflowPhaseIdMap == null) {
      return null;
    }
    return workflowPhaseIdMap.values().stream().map(WorkflowPhase::getServiceId).distinct().collect(toList());
  }

  @Override
  public List<String> getInfraMappingIds() {
    if (workflowPhaseIdMap == null) {
      return null;
    }
    return workflowPhaseIdMap.values().stream().map(WorkflowPhase::getInfraMappingId).distinct().collect(toList());
  }

  @Override
  public void setCloneMetadata(Map<String, String> serviceIdMapping) {
    if (workflowPhaseIdMap == null || serviceIdMapping == null) {
      return;
    }
    workflowPhaseIdMap.values().stream().forEach(workflowPhase -> {
      String serviceId = workflowPhase.getServiceId();
      if (serviceId != null) {
        if (serviceIdMapping.containsKey(serviceId)) {
          workflowPhase.setServiceId(serviceIdMapping.get(serviceId));
        }
      }
      workflowPhase.setInfraMappingId(null);
      workflowPhase.setInfraMappingName(null);
    });

    if (rollbackWorkflowPhaseIdMap == null || serviceIdMapping == null) {
      return;
    }
    rollbackWorkflowPhaseIdMap.values().stream().forEach(rollbackPhase -> {
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
    if (workflowPhases != null) {
      workflowPhaseIds = new ArrayList<>();
      workflowPhaseIdMap = new HashMap<>();

      int i = 0;
      for (WorkflowPhase workflowPhase : workflowPhases) {
        if (isBlank(workflowPhase.getName()) || phaseNamePattern.matcher(workflowPhase.getName()).matches()) {
          workflowPhase.setName(PHASE_NAME_PREFIX + ++i);
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
  public void onLoad() {
    populatePhaseSteps(preDeploymentSteps, getGraph());

    workflowPhases = new ArrayList<>();
    for (String workflowPhaseId : workflowPhaseIds) {
      WorkflowPhase workflowPhase = workflowPhaseIdMap.get(workflowPhaseId);
      workflowPhases.add(workflowPhase);
      workflowPhase.getPhaseSteps().forEach(phaseStep -> { populatePhaseSteps(phaseStep, getGraph()); });
    }
    if (rollbackWorkflowPhaseIdMap != null) {
      rollbackWorkflowPhaseIdMap.values().forEach(workflowPhase -> {
        workflowPhase.getPhaseSteps().forEach(phaseStep -> { populatePhaseSteps(phaseStep, getGraph()); });
      });
    }
    populatePhaseSteps(postDeploymentSteps, getGraph());
    reorderUserVariables();
  }

  /**
   * Re orderes the user variables first by Entity - Enviroment, Service - Service Infra
   */
  private void reorderUserVariables() {
    List<Variable> reorderVariables = new ArrayList<>();
    if (userVariables != null) {
      // First get all Entity type user variables
      List<Variable> entityVariables =
          userVariables.stream().filter(variable -> variable.getEntityType() != null).collect(toList());
      List<Variable> nonEntityVariables =
          userVariables.stream().filter(variable -> variable.getEntityType() == null).collect(toList());
      if (entityVariables != null) {
        for (Variable variable : entityVariables) {
          EntityType entityType = variable.getEntityType();
          if (entityType.equals(ENVIRONMENT)) {
            reorderVariables.add(variable);
            break;
          }
        }
        List<Variable> serviceInfraVariables = new ArrayList<>();
        for (Variable variable : entityVariables) {
          EntityType entityType = variable.getEntityType();
          if (entityType.equals(SERVICE)) {
            serviceInfraVariables.add(variable);
            Optional<Variable> infraVariable = entityVariables.stream()
                                                   .filter(variable1
                                                       -> variable1.getEntityType().equals(INFRASTRUCTURE_MAPPING)
                                                           && variable1.getName().equals(variable.getRelatedField()))
                                                   .findFirst();
            if (infraVariable.isPresent()) {
              serviceInfraVariables.add(infraVariable.get());
            }
          }
        }
        for (Variable variable : entityVariables) {
          EntityType entityType = variable.getEntityType();
          if (entityType.equals(INFRASTRUCTURE_MAPPING)) {
            if (!serviceInfraVariables.stream().anyMatch(variable1 -> variable1.getName().equals(variable.getName()))) {
              serviceInfraVariables.add(variable);
            }
          }
        }
        if (serviceInfraVariables != null) {
          reorderVariables.addAll(serviceInfraVariables);
        }
      }
      if (nonEntityVariables != null) {
        reorderVariables.addAll(nonEntityVariables);
      }
    }
    userVariables = reorderVariables;
  }

  @Override
  public void updateUserVariables() {
    List<String> templateVariables = getTemplateVariables();
    List<Variable> newVariables = new ArrayList<>();
    if (userVariables != null) {
      // First get all Entity type user variables
      // First get all Entity type user variables
      List<Variable> entityVariables =
          userVariables.stream().filter(variable -> variable.getEntityType() != null).collect(toList());
      List<Variable> nonEntityVariables =
          userVariables.stream().filter(variable -> variable.getEntityType() == null).collect(toList());
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

  public void populatePhaseStepIds(WorkflowPhase workflowPhase) {
    if (workflowPhase.getPhaseSteps() == null || workflowPhase.getPhaseSteps().isEmpty()) {
      return;
    }
    workflowPhase.getPhaseSteps().forEach(this ::populatePhaseStepIds);
  }

  public void populatePhaseStepIds(PhaseStep phaseStep) {
    if (phaseStep.getSteps() == null) {
      logger.error("Incorrect arguments to populate phaseStepIds: {}", phaseStep);
      return;
    }
    phaseStep.setStepsIds(phaseStep.getSteps().stream().map(Node::getId).collect(toList()));
  }

  private void populatePhaseSteps(PhaseStep phaseStep, Graph graph) {
    if (phaseStep == null || phaseStep.getUuid() == null || graph == null || graph.getSubworkflows() == null
        || graph.getSubworkflows().get(phaseStep.getUuid()) == null) {
      logger.error("Incorrect arguments to populate phaseStep: {}, graph: {}", phaseStep, graph);
      return;
    }
    if (phaseStep.getStepsIds() == null || phaseStep.getStepsIds().isEmpty()) {
      //      logger.info("Empty stepList for the phaseStep: {}", phaseStep);
      return;
    }

    Graph subWorkflowGraph = graph.getSubworkflows().get(phaseStep.getUuid());
    if (subWorkflowGraph == null) {
      logger.info("No subworkflow found for the phaseStep: {}", phaseStep);
      return;
    }

    Map<String, Node> nodesMap = subWorkflowGraph.getNodesMap();
    phaseStep.setSteps(phaseStep.getStepsIds().stream().map(stepId -> nodesMap.get(stepId)).collect(toList()));
  }

  private Graph generateGraph() {
    String id1 = preDeploymentSteps.getUuid();
    String id2;
    Node preDeploymentNode = preDeploymentSteps.generatePhaseStepNode();
    preDeploymentNode.setOrigin(true);
    Builder graphBuilder =
        aGraph().addNodes(preDeploymentNode).addSubworkflow(id1, preDeploymentSteps.generateSubworkflow(null));

    if (workflowPhases != null) {
      for (WorkflowPhase workflowPhase : workflowPhases) {
        id2 = workflowPhase.getUuid();
        graphBuilder.addNodes(workflowPhase.generatePhaseNode())
            .addLinks(
                aLink().withId(getUuid()).withFrom(id1).withTo(id2).withType(TransitionType.SUCCESS.name()).build())
            .addSubworkflows(workflowPhase.generateSubworkflows());

        if (rollbackWorkflowPhaseIdMap != null && rollbackWorkflowPhaseIdMap.get(workflowPhase.getUuid()) != null) {
          Node rollbackNode = rollbackWorkflowPhaseIdMap.get(workflowPhase.getUuid()).generatePhaseNode();
          graphBuilder.addNodes(rollbackNode)
              .addSubworkflows(rollbackWorkflowPhaseIdMap.get(workflowPhase.getUuid()).generateSubworkflows());
        }
        id1 = id2;
      }
    }
    id2 = postDeploymentSteps.getUuid();
    graphBuilder.addNodes(postDeploymentSteps.generatePhaseStepNode())
        .addLinks(aLink().withId(getUuid()).withFrom(id1).withTo(id2).withType(TransitionType.SUCCESS.name()).build())
        .addSubworkflow(id2, postDeploymentSteps.generateSubworkflow(null));

    return graphBuilder.build();
  }

  @Override
  public boolean validate() {
    setValid(true);
    setValidationMessage(null);
    if (workflowPhases != null) {
      String invalid = "";
      List<String> invalidChildren = workflowPhases.stream()
                                         .filter(workflowPhase -> !workflowPhase.validate())
                                         .map(WorkflowPhase::getName)
                                         .collect(toList());
      if (isNotEmpty(invalidChildren)) {
        setValid(false);
        invalid += invalidChildren.toString();
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
        setValidationMessage(String.format(WORKFLOW_VALIDATION_MESSAGE, invalid));
      }
      validateInframapping();
    }
    return isValid();
  }

  private void validateInframapping() {
    if (workflowPhases != null) {
      List<String> invalidInfraPhaseIds = new ArrayList<>();
      for (WorkflowPhase phase : workflowPhases) {
        if (phase == null || phase.checkInfraTemplatized()) {
          continue;
        }
        if (phase.getInfraMappingId() == null || phase.getInfraMappingId().isEmpty()) {
          invalidInfraPhaseIds.add(phase.getName());
        }
      }
      if (isNotEmpty(invalidInfraPhaseIds)) {
        setValid(false);
        setValidationMessage(String.format(WORKFLOW_INFRAMAPPING_VALIDATION_MESSAGE, invalidInfraPhaseIds.toString()));
      }
    }
  }

  public boolean serviceRepeat(WorkflowPhase workflowPhase) {
    if (workflowPhaseIds == null) {
      return false;
    }
    for (String phaseId : workflowPhaseIds) {
      WorkflowPhase existingPhase = workflowPhaseIdMap.get(phaseId);
      if (existingPhase.getServiceId().equals(workflowPhase.getServiceId())
          && existingPhase.getDeploymentType() == workflowPhase.getDeploymentType()
          && existingPhase.getInfraMappingId() != null
          && existingPhase.getInfraMappingId().equals(workflowPhase.getInfraMappingId())) {
        return true;
      }
    }
    return false;
  }

  @Override
  public OrchestrationWorkflow clone() {
    return aCanaryOrchestrationWorkflow()
        .aCanaryOrchestrationWorkflow()
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

  public static final class CanaryOrchestrationWorkflowBuilder {
    private Graph graph;
    private PhaseStep preDeploymentSteps = new PhaseStep(PhaseStepType.PRE_DEPLOYMENT, PRE_DEPLOYMENT);
    private List<String> workflowPhaseIds = new ArrayList<>();
    private Map<String, WorkflowPhase> workflowPhaseIdMap = new HashMap<>();
    private Map<String, WorkflowPhase> rollbackWorkflowPhaseIdMap = new HashMap<>();
    private List<WorkflowPhase> workflowPhases = new ArrayList<>();
    private PhaseStep postDeploymentSteps = new PhaseStep(PhaseStepType.POST_DEPLOYMENT, POST_DEPLOYMENT);
    private List<NotificationRule> notificationRules = new ArrayList<>();
    private List<FailureStrategy> failureStrategies = new ArrayList<>();
    private List<Variable> systemVariables = new ArrayList<>();
    private List<Variable> userVariables = new ArrayList<>();
    private List<Variable> derivedVariables = new ArrayList<>();
    private Set<EntityType> requiredEntityTypes;

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
      return canaryOrchestrationWorkflow;
    }
  }
}
