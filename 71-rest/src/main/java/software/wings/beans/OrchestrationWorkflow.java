package software.wings.beans;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Arrays.asList;
import static software.wings.beans.EntityType.ENVIRONMENT;
import static software.wings.beans.EntityType.valueOf;
import static software.wings.beans.Variable.VariableBuilder.aVariable;
import static software.wings.beans.VariableType.ENTITY;
import static software.wings.beans.VariableType.TEXT;
import static software.wings.common.Constants.ARTIFACT_TYPE;
import static software.wings.common.Constants.ENTITY_TYPE;
import static software.wings.common.Constants.PARENT_FIELDS;
import static software.wings.common.Constants.RELATED_FIELD;
import static software.wings.common.Constants.STATE_TYPE;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.data.structure.EmptyPredicate;
import software.wings.beans.Variable.VariableBuilder;
import software.wings.common.Constants;
import software.wings.expression.ManagerExpressionEvaluator;
import software.wings.service.impl.workflow.WorkflowServiceTemplateHelper;
import software.wings.sm.State;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * Created by rishi on 3/28/17.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "orchestrationWorkflowType", include = As.PROPERTY)
@JsonSubTypes({
  @JsonSubTypes.Type(value = CanaryOrchestrationWorkflow.class, name = "CANARY")
  , @JsonSubTypes.Type(value = CustomOrchestrationWorkflow.class, name = "CUSTOM"),
      @JsonSubTypes.Type(value = BasicOrchestrationWorkflow.class, name = "BASIC"),
      @JsonSubTypes.Type(value = BlueGreenOrchestrationWorkflow.class, name = "BLUE_GREEN"),
      @JsonSubTypes.Type(value = RollingOrchestrationWorkflow.class, name = "ROLLING"),
      @JsonSubTypes.Type(value = MultiServiceOrchestrationWorkflow.class, name = "MULTI_SERVICE"),
      @JsonSubTypes.Type(value = BuildWorkflow.class, name = "BUILD"),
})
public abstract class OrchestrationWorkflow {
  private OrchestrationWorkflowType orchestrationWorkflowType;

  private boolean valid;

  private String validationMessage;

  private transient List<String> linkedTemplateUuids = new ArrayList<>();

  public OrchestrationWorkflowType getOrchestrationWorkflowType() {
    return orchestrationWorkflowType;
  }

  public void setOrchestrationWorkflowType(OrchestrationWorkflowType orchestrationWorkflowType) {
    this.orchestrationWorkflowType = orchestrationWorkflowType;
  }

  public boolean isValid() {
    return valid;
  }

  public void setValid(boolean valid) {
    this.valid = valid;
  }

  public String getValidationMessage() {
    return validationMessage;
  }

  public void setValidationMessage(String validationMessage) {
    this.validationMessage = validationMessage;
  }

  public abstract List<String> getServiceIds();

  public abstract void onSave();

  public abstract void onLoad();

  public abstract Set<EntityType> getRequiredEntityTypes();

  public abstract void setRequiredEntityTypes(Set<EntityType> requiredEntityTypes);

  public abstract boolean validate();

  public abstract OrchestrationWorkflow cloneInternal();

  public abstract List<Variable> getUserVariables();

  public abstract void setCloneMetadata(Map<String, String> serviceIdMapping);

  public abstract List<String> getInfraMappingIds();

  public abstract boolean needCloudProvider();

  public abstract List<NotificationRule> getNotificationRules();

  public abstract void setNotificationRules(List<NotificationRule> notificationRules);

  @JsonIgnore
  public List<String> getTemplateVariables() {
    return new ArrayList<>();
  }

  /**
   * Checks if the workflow is templatized or not
   *
   * @return
   */
  @JsonIgnore
  public boolean isTemplatized() {
    List<Variable> userVariables = getUserVariables();
    if (isEmpty(userVariables)) {
      return false;
    }
    return userVariables.stream().anyMatch(variable -> !variable.isFixed());
  }

  @JsonIgnore
  public List<String> getTemplatizedServiceIds() {
    return asList();
  }

  @JsonIgnore
  public List<String> getTemplatizedInfraMappingIds() {
    return asList();
  }

  public void updateUserVariables() {}

  /**
   * Checks if any one of InfraMapping is templatized
   *
   * @return
   */
  @JsonIgnore
  public boolean isServiceTemplatized() {
    return false;
  }

  /**
   * Checks if any one of Env is templatized
   *
   * @return
   */
  @JsonIgnore
  public boolean isInfraMappingTemplatized() {
    return false;
  }

  public void addToUserVariables(State state) {
    addToUserVariables(state.getTemplateExpressions(), state.getStateType(), state.getName(), state);
  }

  /***
   * Add template expressions to workflow variables
   */
  @SuppressFBWarnings("UC_USELESS_OBJECT")
  public void addToUserVariables(
      List<TemplateExpression> templateExpressions, String stateType, String name, State state) {
    if (isEmpty(templateExpressions)) {
      return;
    }
    for (TemplateExpression templateExpression : templateExpressions) {
      EntityType entityType = null;
      String artifactType = null;
      String relatedField = null;
      Map<String, Object> metadata = templateExpression.getMetadata();
      if (metadata != null) {
        if (metadata.get(ENTITY_TYPE) != null) {
          entityType = valueOf((String) metadata.get(ENTITY_TYPE));
        }
        if (metadata.get(ARTIFACT_TYPE) != null) {
          artifactType = (String) metadata.get(ARTIFACT_TYPE);
        }
        if (metadata.get(RELATED_FIELD) != null) {
          relatedField = (String) metadata.get(RELATED_FIELD);
        }
      }
      String expression = templateExpression.getExpression();
      Matcher matcher = ManagerExpressionEvaluator.wingsVariablePattern.matcher(expression);
      if (relatedField != null) {
        Matcher relatedFieldMatcher = ManagerExpressionEvaluator.wingsVariablePattern.matcher(relatedField);
        if (relatedFieldMatcher.matches()) {
          relatedField = relatedField.substring(2, relatedField.length() - 1);
        }
      }
      if (matcher.matches()) {
        expression =
            getTemplateExpressionName(matcher.group(0).substring(2, matcher.group(0).length() - 1), entityType);
      } else {
        expression = getTemplateExpressionName(expression, entityType);
      }
      Variable variable = contains(getUserVariables(), expression);
      Map<String, String> parentTemplateFields =
          state == null ? null : state.parentTemplateFields(templateExpression.getFieldName());
      if (variable == null) {
        VariableBuilder variableBuilder = aVariable()
                                              .withName(expression)
                                              .withEntityType(entityType)
                                              .withArtifactType(artifactType)
                                              .withRelatedField(relatedField)
                                              .withType(entityType != null ? ENTITY : TEXT)
                                              .withMandatory(entityType != null);

        variableBuilder.withParentFields(parentTemplateFields);
        if (isNotEmpty(stateType)) {
          variableBuilder.withStateType(stateType);
        }
        // Set the description
        variable = variableBuilder.build();
        setVariableDescription(variable, name);
        getUserVariables().add(variable);
      } else {
        Map<String, Object> variableMetadata = variable.getMetadata();
        if (variableMetadata == null) {
          variableMetadata = new HashMap<>();
        }
        variableMetadata.put(ENTITY_TYPE, entityType);
        if (isNotEmpty(artifactType)) {
          variableMetadata.put(ARTIFACT_TYPE, artifactType);
        }
        if (isNotEmpty(relatedField)) {
          variableMetadata.put(RELATED_FIELD, relatedField);
        }
        if (ENVIRONMENT != entityType && isNotEmpty(stateType)) {
          variableMetadata.put(STATE_TYPE, stateType);
        }
        variable.setMandatory(entityType != null);
        if (isEmpty(parentTemplateFields)) {
          variableMetadata.remove(Constants.PARENT_FIELDS);
        } else {
          variableMetadata.put(PARENT_FIELDS, parentTemplateFields);
        }
        setVariableDescription(variable, name);
      }
      getTemplateVariables().add(expression);
    }
  }

  /**
   * Adds template expression as workflow variables
   *
   * @param templateExpressions
   */
  public void addToUserVariables(List<TemplateExpression> templateExpressions) {
    addToUserVariables(templateExpressions, null, null, null);
  }

  private Variable contains(List<Variable> userVariables, String name) {
    if (userVariables == null) {
      return null;
    }
    return userVariables.stream()
        .filter(variable -> variable != null && variable.getName() != null && variable.getName().equals(name))
        .findFirst()
        .orElse(null);
  }

  private String getTemplateExpressionName(String templateVariable, EntityType entityType) {
    return WorkflowServiceTemplateHelper.validatetAndGetVariable(templateVariable, entityType);
  }

  /**
   * Set template description
   *
   * @param variable
   * @param stateName
   */
  private void setVariableDescription(Variable variable, String stateName) {
    variable.setDescription(WorkflowServiceTemplateHelper.getVariableDescription(
        variable.getEntityType(), getOrchestrationWorkflowType(), stateName));
  }

  public void addTemplateUuid(String templateUuid) {
    if (EmptyPredicate.isEmpty(linkedTemplateUuids)) {
      linkedTemplateUuids = new ArrayList<>();
    }
    linkedTemplateUuids.add(templateUuid);
  }

  public List<String> getLinkedTemplateUuids() {
    return linkedTemplateUuids;
  }
}
