package software.wings.service.impl.yaml.handler.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;
import static software.wings.beans.yaml.YamlConstants.PATH_DELIMITER;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Application;
import software.wings.beans.trigger.Action;
import software.wings.beans.trigger.Condition;
import software.wings.beans.trigger.DeploymentTrigger;
import software.wings.beans.yaml.Change;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlType;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.trigger.DeploymentTriggerService;
import software.wings.yaml.trigger.ActionYaml;
import software.wings.yaml.trigger.ConditionYaml;
import software.wings.yaml.trigger.DeploymentTriggerYaml;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@OwnedBy(CDC)
@Singleton
@Data
@EqualsAndHashCode(callSuper = false)
@Slf4j
public class DeploymentTriggerYamlHandler extends BaseYamlHandler<DeploymentTriggerYaml, DeploymentTrigger> {
  @Inject YamlHelper yamlHelper;
  @Inject DeploymentTriggerService deploymentTriggerService;
  @Inject YamlHandlerFactory yamlHandlerFactory;

  @Override
  public void delete(ChangeContext<DeploymentTriggerYaml> changeContext) {
    String accountId = changeContext.getChange().getAccountId();
    String yamlFilePath = changeContext.getChange().getFilePath();
    Optional<Application> optionalApplication = yamlHelper.getApplicationIfPresent(accountId, yamlFilePath);
    if (!optionalApplication.isPresent()) {
      return;
    }

    String appId = yamlHelper.getAppId(accountId, yamlFilePath);

    DeploymentTrigger trigger = yamlHelper.getDeploymentTrigger(appId, yamlFilePath);
    if (trigger == null) {
      return;
    }

    deploymentTriggerService.delete(optionalApplication.get().getUuid(), trigger.getUuid());
  }

  @Override
  public DeploymentTriggerYaml toYaml(DeploymentTrigger bean, String appId) {
    notNullCheck("Trigger Condition cannot be null", bean.getCondition());
    ConditionYamlHandler conditionYamlHandler =
        yamlHandlerFactory.getYamlHandler(YamlType.CONDITION, bean.getCondition().getType().name());

    ConditionYaml conditionYaml = conditionYamlHandler.toYaml(bean.getCondition(), appId);

    DeploymentTriggerYaml yaml = DeploymentTriggerYaml.builder()
                                     .description(bean.getDescription())
                                     .condition(Collections.singletonList(conditionYaml))
                                     .harnessApiVersion(getHarnessApiVersion())
                                     .triggerDisabled(bean.isTriggerDisabled())
                                     .build();

    if (bean.getAction() != null) {
      ActionYamlHandler actionYamlHandler =
          yamlHandlerFactory.getYamlHandler(YamlType.ACTION, bean.getAction().getActionType().name());

      ActionYaml actionYaml = actionYamlHandler.toYaml(bean.getAction(), appId);
      yaml.setAction(Collections.singletonList(actionYaml));
    }

    updateYamlWithAdditionalInfo(bean, appId, yaml);

    return yaml;
  }

  @Override
  public DeploymentTrigger upsertFromYaml(
      ChangeContext<DeploymentTriggerYaml> changeContext, List<ChangeContext> changeSetContext) {
    Change change = changeContext.getChange();
    String appId = yamlHelper.getAppId(change.getAccountId(), change.getFilePath());
    notNullCheck("Could not locate app info in file path:" + change.getFilePath(), appId, USER);

    DeploymentTrigger existingTrigger = yamlHelper.getDeploymentTrigger(appId, change.getFilePath());

    DeploymentTrigger trigger = toBean(appId, changeContext, changeSetContext);
    if (existingTrigger == null) {
      trigger = deploymentTriggerService.save(trigger, false);
    } else {
      trigger = deploymentTriggerService.update(trigger);
    }

    changeContext.setEntity(trigger);
    return trigger;
  }

  private DeploymentTrigger toBean(
      String appId, ChangeContext<DeploymentTriggerYaml> changeContext, List<ChangeContext> changeSetContext) {
    DeploymentTriggerYaml yaml = changeContext.getYaml();
    Change change = changeContext.getChange();

    DeploymentTrigger trigger = yamlHelper.getDeploymentTrigger(appId, change.getFilePath());
    String uuid = null;
    if (trigger != null) {
      uuid = trigger.getUuid();
    }

    String name = yamlHelper.extractEntityNameFromYamlPath(
        YamlType.TRIGGER.getPathExpression(), change.getFilePath(), PATH_DELIMITER);

    ConditionYaml conditionYaml = yaml.getCondition().get(0);
    ConditionYamlHandler conditionYamlHandler =
        yamlHandlerFactory.getYamlHandler(YamlType.CONDITION, conditionYaml.getType());
    ChangeContext.Builder clonedConditionContext = cloneFileChangeContext(changeContext, conditionYaml);

    Condition condition = conditionYamlHandler.upsertFromYaml(clonedConditionContext.build(), changeSetContext);

    Action action = null;
    if (EmptyPredicate.isNotEmpty(yaml.getAction())) {
      ActionYaml actionYaml = yaml.getAction().get(0);
      ActionYamlHandler actionYamlHandler = yamlHandlerFactory.getYamlHandler(YamlType.ACTION, actionYaml.getType());

      ChangeContext.Builder clonedActionContext = cloneFileChangeContext(changeContext, actionYaml);
      action = actionYamlHandler.upsertFromYaml(clonedActionContext.build(), changeSetContext);
    }

    return DeploymentTrigger.builder()
        .name(name)
        .uuid(uuid)
        .condition(condition)
        .action(action)
        .appId(appId)
        .accountId(change.getAccountId())
        .triggerDisabled(yaml.isTriggerDisabled())
        .description(yaml.getDescription())
        .build();
  }

  @Override
  public Class getYamlClass() {
    return DeploymentTriggerYaml.class;
  }

  @Override
  public DeploymentTrigger get(String accountId, String yamlFilePath) {
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);
    notNullCheck("Could not lookup app for the yaml file: " + yamlFilePath, appId);
    return yamlHelper.getDeploymentTrigger(appId, yamlFilePath);
  }
}
