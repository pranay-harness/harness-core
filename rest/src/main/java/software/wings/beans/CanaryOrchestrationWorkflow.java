package software.wings.beans;

import static java.util.stream.Collectors.toList;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.Graph.Builder.aGraph;
import static software.wings.beans.Graph.Link.Builder.aLink;
import static software.wings.beans.Variable.VariableBuilder.aVariable;
import static software.wings.beans.VariableType.ENTITY;
import static software.wings.beans.VariableType.TEXT;
import static software.wings.common.Constants.ARTIFACT_TYPE;
import static software.wings.common.Constants.ENTITY_TYPE;
import static software.wings.common.Constants.PHASE_NAME_PREFIX;
import static software.wings.common.Constants.POST_DEPLOYMENT;
import static software.wings.common.Constants.PRE_DEPLOYMENT;
import static software.wings.common.Constants.RELATED_FIELD;
import static software.wings.common.Constants.ROLLBACK_PREFIX;
import static software.wings.common.Constants.WORKFLOW_VALIDATION_MESSAGE;
import static software.wings.common.UUIDGenerator.getUuid;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Graph.Builder;
import software.wings.beans.Graph.Node;
import software.wings.exception.WingsException;
import software.wings.sm.TransitionType;
import software.wings.utils.ExpressionEvaluator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

/**
 * Created by rishi on 12/21/16.
 */
@JsonTypeName("CANARY")
public class CanaryOrchestrationWorkflow extends CustomOrchestrationWorkflow {
  public CanaryOrchestrationWorkflow() {
    setOrchestrationWorkflowType(OrchestrationWorkflowType.CANARY);
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
    return workflowPhaseIdMap.values()
        .stream()
        .map(WorkflowPhase::getServiceId)
        .distinct()
        .collect(Collectors.toList());
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
        workflowPhase.setName(PHASE_NAME_PREFIX + ++i);
        workflowPhaseIds.add(workflowPhase.getUuid());
        workflowPhaseIdMap.put(workflowPhase.getUuid(), workflowPhase);
        List<TemplateExpression> templateExpressions = workflowPhase.getTemplateExpressions();
        if (templateExpressions != null) {
          templateExpressions.stream().forEach(templateExpression -> templateExpression.setExpressionAllowed(false));
          addToUserVariables(templateExpressions);
        }
        populatePhaseStepIds(workflowPhase);

        WorkflowPhase rollbackPhase = rollbackWorkflowPhaseIdMap.get(workflowPhase.getUuid());
        rollbackPhase.setName(ROLLBACK_PREFIX + workflowPhase.getName());
        rollbackPhase.setPhaseNameForRollback(workflowPhase.getName());
        populatePhaseStepIds(rollbackPhase);
      }
    }
    populatePhaseStepIds(postDeploymentSteps);
    setGraph(generateGraph());
  }

  /**
   * Adds template expression as workflow variables
   * @param templateExpressions
   */
  public void addToUserVariables(List<TemplateExpression> templateExpressions) {
    if (templateExpressions == null || templateExpressions.isEmpty()) {
      return;
    }
    for (TemplateExpression templateExpression : templateExpressions) {
      EntityType entityType = null;
      String artifactType = null;
      String relatedField = null;
      Map<String, Object> metadata = templateExpression.getMetadata();
      if (metadata != null) {
        if (metadata.get(ENTITY_TYPE) != null) {
          entityType = EntityType.valueOf((String) metadata.get(ENTITY_TYPE));
        }
        if (metadata.get(ARTIFACT_TYPE) != null) {
          artifactType = (String) metadata.get(ARTIFACT_TYPE);
        }
        if (metadata.get(RELATED_FIELD) != null) {
          relatedField = (String) metadata.get(RELATED_FIELD);
        }
      }
      String expression = templateExpression.getExpression();
      Matcher matcher = ExpressionEvaluator.wingsVariablePattern.matcher(expression);
      if (relatedField != null) {
        Matcher relatedFieldMatcher = ExpressionEvaluator.wingsVariablePattern.matcher(relatedField);
        if (relatedFieldMatcher.matches()) {
          relatedField = relatedField.substring(2, relatedField.length() - 1);
        }
      }
      if (matcher.matches()) {
        String templateVariable = matcher.group(0);
        templateVariable = templateVariable.substring(2, templateVariable.length() - 1);
        templateVariable = getTemplateExpressionName(templateExpression, templateVariable);
        if (!userVariables.contains(templateVariable)) {
          userVariables.add(aVariable()
                                .withName(templateVariable)
                                .withEntityType(entityType)
                                .withArtifactType(artifactType)
                                .withRelatedField(relatedField)
                                .withType(entityType != null ? ENTITY : TEXT)
                                .withMandatory(true)
                                .build());
        }
      } else {
        expression = getTemplateExpressionName(templateExpression, expression);
        if (!userVariables.contains(expression)) {
          userVariables.add(aVariable()
                                .withName(expression)
                                .withEntityType(entityType)
                                .withArtifactType(artifactType)
                                .withRelatedField(relatedField)
                                .withType(entityType != null ? ENTITY : TEXT)
                                .build());
        }
      }
    }
  }

  private String getTemplateExpressionName(TemplateExpression templateExpression, String templateVariable) {
    if (templateVariable != null) {
      if (templateVariable.contains(".")) {
        if (templateVariable.startsWith("workflow.variables.")) {
          return templateVariable.replace("workflow.variables.", "");
        } else if (!templateExpression.isExpressionAllowed()) {
          throw new WingsException(ErrorCode.INVALID_REQUEST, "message",
              "Invalid template expression :" + templateExpression.getExpression()
                  + " for fieldName:" + templateExpression.getFieldName());
        }
      } // Check for proper variable regex
    }
    return templateVariable;
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
                                         .collect(Collectors.toList());
      if (invalidChildren != null && !invalidChildren.isEmpty()) {
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
    }
    return isValid();
  }

  @Override
  public OrchestrationWorkflow clone() {
    return aCanaryOrchestrationWorkflow()
        .aCanaryOrchestrationWorkflow()
        .withGraph(getGraph())
        .withPreDeploymentSteps(getPreDeploymentSteps())
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
