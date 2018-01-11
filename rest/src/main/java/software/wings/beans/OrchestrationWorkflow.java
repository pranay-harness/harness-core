package software.wings.beans;

import static java.util.Arrays.asList;
import static software.wings.beans.EntityType.ENVIRONMENT;
import static software.wings.beans.EntityType.INFRASTRUCTURE_MAPPING;
import static software.wings.beans.EntityType.SERVICE;
import static software.wings.beans.OrchestrationWorkflowType.BASIC;
import static software.wings.beans.Variable.VariableBuilder.aVariable;
import static software.wings.beans.VariableType.ENTITY;
import static software.wings.beans.VariableType.TEXT;
import static software.wings.common.Constants.ARTIFACT_TYPE;
import static software.wings.common.Constants.ENTITY_TYPE;
import static software.wings.common.Constants.RELATED_FIELD;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import org.apache.commons.collections.CollectionUtils;
import software.wings.exception.WingsException;
import software.wings.utils.ExpressionEvaluator;

import java.util.ArrayList;
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
      @JsonSubTypes.Type(value = MultiServiceOrchestrationWorkflow.class, name = "MULTI_SERVICE"),
      @JsonSubTypes.Type(value = BuildWorkflow.class, name = "BUILD"),
})
public abstract class OrchestrationWorkflow {
  private OrchestrationWorkflowType orchestrationWorkflowType;

  private boolean valid;

  private String validationMessage;

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

  public abstract OrchestrationWorkflow clone();

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
   * @return
   */
  @JsonIgnore
  public boolean isTemplatized() {
    List<Variable> userVariables = getUserVariables();
    if (CollectionUtils.isEmpty(userVariables)) {
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
   * @return
   */
  @JsonIgnore
  public boolean isServiceTemplatized() {
    return false;
  }

  /**
   * Checks if any one of Env is templatized
   * @return
   */
  @JsonIgnore
  public boolean isInfraMappingTemplatized() {
    return false;
  }
  /***
   * Add template expressions to workflow variables
   */

  public void addToUserVariables(List<TemplateExpression> templateExpressions, String stateType, String name) {
    if (CollectionUtils.isEmpty(templateExpressions)) {
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
        expression = getTemplateExpressionName(
            templateExpression, matcher.group(0).substring(2, matcher.group(0).length() - 1), entityType, stateType);
      } else {
        expression = getTemplateExpressionName(templateExpression, expression, entityType, stateType);
      }
      if (!contains(getUserVariables(), expression)) {
        Variable variable = aVariable()
                                .withName(expression)
                                .withEntityType(entityType)
                                .withArtifactType(artifactType)
                                .withRelatedField(relatedField)
                                .withType(entityType != null ? ENTITY : TEXT)
                                .withMandatory(entityType != null)
                                .build();

        // Set the description
        setVariableDescription(variable, name);
        getUserVariables().add(variable);
      }
      getTemplateVariables().add(expression);
    }
  }
  /**
   * Adds template expression as workflow variables
   * @param templateExpressions
   */
  public void addToUserVariables(List<TemplateExpression> templateExpressions) {
    addToUserVariables(templateExpressions, null, null);
  }

  private boolean contains(List<Variable> userVariables, String name) {
    return userVariables != null
        && userVariables.stream().anyMatch(
               variable -> variable != null && variable.getName() != null && variable.getName().equals(name));
  }

  private String getTemplateExpressionName(
      TemplateExpression templateExpression, String templateVariable, EntityType entityType, String stateType) {
    if (templateVariable != null) {
      if (templateVariable.startsWith("workflow.variables.")) {
        templateVariable = templateVariable.replace("workflow.variables.", "");
      }
    }
    Matcher matcher = ExpressionEvaluator.variableNamePattern.matcher(templateVariable);
    // check if template variable contains special character
    if (entityType != null) {
      if (!matcher.matches()) {
        throw new WingsException(ErrorCode.INVALID_ARGUMENT)
            .addParam(
                "args", "Template expression:" + templateExpression.getExpression() + " contains special characters");
      }
    } else if (stateType != null) {
      // TODO: Check if it can contain other expressions
    }
    return templateVariable;
  }

  /**
   * Set template descripton
   * @param variable
   * @param stateName
   */
  private void setVariableDescription(Variable variable, String stateName) {
    EntityType entityType = variable.getEntityType();
    if (entityType != null) {
      if (entityType.equals(ENVIRONMENT)) {
        variable.setDescription("Variable for Environment entity");
      } else if (entityType.equals(SERVICE)) {
        if (getOrchestrationWorkflowType().equals(BASIC)) {
          variable.setDescription("Variable for Service entity");
        } else {
          variable.setDescription("Variable for Service entity in " + stateName);
        }
      } else if (entityType.equals(INFRASTRUCTURE_MAPPING)) {
        if (getOrchestrationWorkflowType().equals(BASIC)) {
          variable.setDescription("Variable for Service Infra-strucuture entity");
        } else {
          variable.setDescription("Variable for Service Infra-strucuture entity " + stateName);
        }
      }
    }
  }
}
