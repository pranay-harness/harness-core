/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static software.wings.beans.EntityType.ENVIRONMENT;
import static software.wings.beans.EntityType.USER_GROUP;
import static software.wings.beans.EntityType.valueOf;
import static software.wings.beans.Variable.VariableBuilder.aVariable;
import static software.wings.beans.VariableType.ENTITY;
import static software.wings.beans.VariableType.TEXT;

import static java.util.Arrays.asList;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.OrchestrationWorkflowType;
import io.harness.data.structure.EmptyPredicate;

import software.wings.beans.Variable.VariableBuilder;
import software.wings.beans.concurrency.ConcurrencyStrategy;
import software.wings.expression.ManagerExpressionEvaluator;
import software.wings.service.impl.workflow.WorkflowServiceTemplateHelper;
import software.wings.sm.State;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import lombok.Getter;
import lombok.Setter;

/**
 * Created by rishi on 3/28/17.
 */
@OwnedBy(CDC)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "orchestrationWorkflowType", include = As.PROPERTY)
@JsonSubTypes({
  @JsonSubTypes.Type(value = CanaryOrchestrationWorkflow.class, name = "CANARY")
  , @JsonSubTypes.Type(value = CustomOrchestrationWorkflow.class, name = "CUSTOM"),
      @JsonSubTypes.Type(value = BasicOrchestrationWorkflow.class, name = "BASIC"),
      @JsonSubTypes.Type(value = BlueGreenOrchestrationWorkflow.class, name = "BLUE_GREEN"),
      @JsonSubTypes.Type(value = EcsBlueGreenOrchestrationWorkflow.class, name = "BLUE_GREEN_ECS"),
      @JsonSubTypes.Type(value = RollingOrchestrationWorkflow.class, name = "ROLLING"),
      @JsonSubTypes.Type(value = MultiServiceOrchestrationWorkflow.class, name = "MULTI_SERVICE"),
      @JsonSubTypes.Type(value = BuildWorkflow.class, name = "BUILD"),
})
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public abstract class OrchestrationWorkflow {
  @Getter @Setter protected ConcurrencyStrategy concurrencyStrategy;
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

  public abstract void onLoad(Workflow workflow);

  public abstract void setTransientFields(Workflow workflow);

  public abstract Set<EntityType> getRequiredEntityTypes();

  public abstract void setRequiredEntityTypes(Set<EntityType> requiredEntityTypes);

  public abstract boolean validate();

  public abstract OrchestrationWorkflow cloneInternal();

  public abstract List<Variable> getUserVariables();

  public abstract void setCloneMetadata(Map<String, String> serviceIdMapping);

  public abstract List<String> getInfraMappingIds();

  public abstract List<String> getInfraDefinitionIds();

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
  public boolean checkTemplatized() {
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

  @JsonIgnore
  public List<String> getTemplatizedInfraDefinitionIds() {
    return asList();
  }

  public abstract void updateUserVariables();

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

  @JsonIgnore
  public boolean isInfraDefinitionTemplatized() {
    return false;
  }

  public void addToUserVariables(State state) {
    addToUserVariables(state.getTemplateExpressions(), state.getStateType(), state.getName(), state);
  }

  /***
   * Add template expressions to workflow variables
   */
  //
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
        if (metadata.get(Variable.ENTITY_TYPE) != null) {
          entityType = valueOf((String) metadata.get(Variable.ENTITY_TYPE));
        }
        if (metadata.get(Variable.ARTIFACT_TYPE) != null) {
          artifactType = (String) metadata.get(Variable.ARTIFACT_TYPE);
        }
        if (metadata.get(Variable.RELATED_FIELD) != null) {
          relatedField = (String) metadata.get(Variable.RELATED_FIELD);
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
                                              .name(expression)
                                              .entityType(entityType)
                                              .artifactType(artifactType)
                                              .relatedField(relatedField)
                                              .type(entityType != null ? ENTITY : TEXT)
                                              .mandatory(entityType != null);

        variableBuilder.parentFields(parentTemplateFields);
        if (isNotEmpty(stateType)) {
          variableBuilder.stateType(stateType);
        }
        // Set the description
        variable = variableBuilder.build();
        setVariableDescription(variable, name);
        checkMultiValuesAllowed(variable, entityType);
        if (getUserVariables() == null) {
          return;
        }
        getUserVariables().add(variable);
      } else {
        Map<String, Object> variableMetadata = variable.getMetadata();
        if (variableMetadata == null) {
          variableMetadata = new HashMap<>();
          variable.setMetadata(variableMetadata);
        }
        variableMetadata.put(Variable.ENTITY_TYPE, entityType);
        if (isNotEmpty(artifactType)) {
          variableMetadata.put(Variable.ARTIFACT_TYPE, artifactType);
        }
        if (isNotEmpty(relatedField)) {
          variableMetadata.put(Variable.RELATED_FIELD, relatedField);
        }
        if (ENVIRONMENT != entityType && isNotEmpty(stateType)) {
          variableMetadata.put(Variable.STATE_TYPE, stateType);
        }
        variable.setMandatory(entityType != null);
        checkMultiValuesAllowed(variable, entityType);
        if (isEmpty(parentTemplateFields)) {
          variableMetadata.remove(Variable.PARENT_FIELDS);
        } else {
          variableMetadata.put(Variable.PARENT_FIELDS, parentTemplateFields);
        }
        setVariableDescription(variable, name);
      }
      getTemplateVariables().add(expression);
    }
  }

  private void checkMultiValuesAllowed(Variable variable, EntityType entityType) {
    if (USER_GROUP == entityType) {
      variable.setAllowMultipleValues(true);
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
    return WorkflowServiceTemplateHelper.validateAndGetVariable(templateVariable, entityType);
  }

  /**
   * Set template description
   *
   * @param variable
   * @param stateName
   */
  private void setVariableDescription(Variable variable, String stateName) {
    variable.setDescription(WorkflowServiceTemplateHelper.getVariableDescription(
        variable.obtainEntityType(), getOrchestrationWorkflowType(), stateName));
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
