package software.wings.beans;

import static software.wings.beans.Variable.VariableBuilder.aVariable;
import static software.wings.beans.VariableType.ENTITY;
import static software.wings.beans.VariableType.TEXT;
import static software.wings.common.Constants.ARTIFACT_TYPE;
import static software.wings.common.Constants.ENTITY_TYPE;
import static software.wings.common.Constants.RELATED_FIELD;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import software.wings.exception.WingsException;
import software.wings.utils.ExpressionEvaluator;

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
      @JsonSubTypes.Type(value = MultiServiceOrchestrationWorkflow.class, name = "MULTI_SERVICE")
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
        if (!contains(getUserVariables(), templateVariable)) {
          getUserVariables().add(aVariable()
                                     .withName(templateVariable)
                                     .withEntityType(entityType)
                                     .withArtifactType(artifactType)
                                     .withRelatedField(relatedField)
                                     .withType(entityType != null ? ENTITY : TEXT)
                                     .withMandatory(templateExpression.isMandatory())
                                     .build());
        }
      } else {
        expression = getTemplateExpressionName(templateExpression, expression);
        if (!contains(getUserVariables(), expression)) {
          getUserVariables().add(aVariable()
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

  private boolean contains(List<Variable> userVariables, String name) {
    return userVariables.stream().anyMatch(variable -> variable.getName().equals(name));
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
}
