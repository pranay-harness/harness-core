package software.wings.service.impl.yaml.handler.trigger;

import static software.wings.beans.Variable.VariableBuilder.aVariable;

import com.google.inject.Inject;

import io.harness.eraro.ErrorCode;
import io.harness.exception.TriggerException;
import software.wings.beans.EntityType;
import software.wings.beans.Variable;
import software.wings.beans.trigger.Action;
import software.wings.beans.trigger.TriggerArgs;
import software.wings.beans.trigger.TriggerArtifactSelectionValue;
import software.wings.beans.trigger.TriggerArtifactVariable;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlType;
import software.wings.service.impl.yaml.WorkflowYAMLHelper;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.yaml.trigger.ActionYaml;
import software.wings.yaml.trigger.TriggerArtifactSelectionValueYaml;
import software.wings.yaml.trigger.TriggerArtifactVariableYaml;
import software.wings.yaml.trigger.TriggerVariableYaml;

import java.util.Collections;
import java.util.List;

public abstract class ActionYamlHandler<Y extends ActionYaml> extends BaseYamlHandler<Y, Action> {
  @Inject protected YamlHandlerFactory yamlHandlerFactory;
  @Inject protected WorkflowYAMLHelper workflowYAMLHelper;
  @Inject protected YamlHelper yamlHelper;

  @Override
  public void delete(ChangeContext<Y> changeContext) {}
  @Override
  public Action get(String accountId, String yamlFilePath) {
    throw new TriggerException(ErrorCode.UNSUPPORTED_OPERATION_EXCEPTION);
  }

  @Override public abstract Y toYaml(Action bean, String appId);

  @Override public abstract Action upsertFromYaml(ChangeContext<Y> changeContext, List<ChangeContext> changeSetContext);

  protected void getArtifactVariableYaml(
      String appId, TriggerArgs triggerArgs, List<TriggerArtifactVariableYaml> triggerArtifactVariableYamls) {
    for (TriggerArtifactVariable triggerArtifactVariable : triggerArgs.getTriggerArtifactVariables()) {
      TriggerArtifactValueYamlHandler triggerArtifactValueYamlHandler =
          yamlHandlerFactory.getYamlHandler(YamlType.TRIGGER_ARTIFACT_VALUE,
              triggerArtifactVariable.getVariableValue().getArtifactSelectionType().name());

      TriggerArtifactSelectionValueYaml valueYaml =
          triggerArtifactValueYamlHandler.toYaml(triggerArtifactVariable.getVariableValue(), appId);

      TriggerArtifactVariableYaml triggerArtifactVariableYaml =
          TriggerArtifactVariableYaml.builder()
              .variableName(triggerArtifactVariable.getVariableName())
              .entityName(triggerArtifactVariable.getEntityName())
              .entityType(triggerArtifactVariable.getEntityType().name())
              .variableValue(Collections.singletonList(valueYaml))
              .build();
      triggerArtifactVariableYamls.add(triggerArtifactVariableYaml);
    }
  }

  protected void getArtifactVariableBean(ChangeContext<Y> changeContext, List<ChangeContext> changeSetContext,
      List<TriggerArtifactVariableYaml> triggerArtifactVariableYamls,
      List<TriggerArtifactVariable> triggerArtifactVariables) {
    for (TriggerArtifactVariableYaml triggerArtifactVariableYaml : triggerArtifactVariableYamls) {
      TriggerArtifactValueYamlHandler triggerArtifactValueYamlHandler = yamlHandlerFactory.getYamlHandler(
          YamlType.TRIGGER_ARTIFACT_VALUE, triggerArtifactVariableYaml.getVariableValue().get(0).getType());

      ChangeContext.Builder clonedContext =
          cloneFileChangeContext(changeContext, triggerArtifactVariableYaml.getVariableValue().get(0));
      TriggerArtifactSelectionValue value =
          triggerArtifactValueYamlHandler.upsertFromYaml(clonedContext.build(), changeSetContext);

      TriggerArtifactVariable triggerArtifactVariable =
          TriggerArtifactVariable.builder()
              .variableName(triggerArtifactVariableYaml.getVariableName())
              .entityName(triggerArtifactVariableYaml.getEntityName())
              .entityType(EntityType.valueOf(triggerArtifactVariableYaml.getEntityType()))
              .variableValue(value)
              .build();
      triggerArtifactVariables.add(triggerArtifactVariable);
    }
  }

  protected void getTriggerVariablesYaml(
      String appId, TriggerArgs triggerArgs, List<TriggerVariableYaml> variableYamls) {
    for (Variable variable : triggerArgs.getVariables()) {
      EntityType entityType = variable.obtainEntityType();
      TriggerVariableYaml variableYaml = TriggerVariableYaml.builder()
                                             .name(variable.getName())
                                             .entityType(entityType != null ? entityType.name() : null)
                                             .build();
      String entryValue = variable.getValue();

      String yamlValue = workflowYAMLHelper.getWorkflowVariableValueYaml(appId, entryValue, entityType);
      if (yamlValue != null) {
        variableYaml.setValue(yamlValue);
        variableYamls.add(variableYaml);
      }
    }
  }

  protected void getTriggerVariablesBean(String accountId, String envId, String appId,
      List<TriggerVariableYaml> triggerVariableYamls, List<Variable> variables) {
    for (TriggerVariableYaml variableYaml : triggerVariableYamls) {
      EntityType entityType = EntityType.valueOf(variableYaml.getEntityType());
      String variableName = variableYaml.getName();
      String variableValue = variableYaml.getValue();

      String beanValue = workflowYAMLHelper.getWorkflowVariableValueBean(
          accountId, envId, appId, variableYaml.getEntityType(), variableValue);
      Variable variable = aVariable().name(variableName).entityType(entityType).build();
      if (beanValue != null) {
        variableYaml.setValue(beanValue);
        variables.add(variable);
      }
    }
  }
}
