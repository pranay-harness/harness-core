package software.wings.service.impl.yaml.handler.notification;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.ErrorCode;
import software.wings.beans.ExecutionScope;
import software.wings.beans.NotificationGroup;
import software.wings.beans.NotificationRule;
import software.wings.beans.NotificationRule.NotificationRuleBuilder;
import software.wings.beans.NotificationRule.Yaml;
import software.wings.beans.ObjectType;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlType;
import software.wings.exception.HarnessException;
import software.wings.exception.WingsException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.sm.ExecutionStatus;
import software.wings.utils.Util;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author rktummala on 10/28/17
 */
@Singleton
public class NotificationRulesYamlHandler extends BaseYamlHandler<NotificationRule.Yaml, NotificationRule> {
  @Inject YamlHandlerFactory yamlHandlerFactory;

  private NotificationRule toBean(ChangeContext<Yaml> changeContext, List<ChangeContext> changeContextList)
      throws HarnessException {
    Yaml yaml = changeContext.getYaml();
    ExecutionScope executionScope = Util.getEnumFromString(ExecutionScope.class, yaml.getExecutionScope());

    List<NotificationGroup> notificationGroups = Lists.newArrayList();
    if (yaml.getNotificationGroups() != null) {
      BaseYamlHandler notificationGroupYamlHandler =
          yamlHandlerFactory.getYamlHandler(YamlType.NOTIFICATION_GROUP, ObjectType.NOTIFICATION_GROUP);
      notificationGroups = yaml.getNotificationGroups()
                               .stream()
                               .map(notificationGroup -> {
                                 try {
                                   ChangeContext.Builder clonedContext =
                                       cloneFileChangeContext(changeContext, notificationGroup);
                                   return (NotificationGroup) notificationGroupYamlHandler.upsertFromYaml(
                                       clonedContext.build(), changeContextList);
                                 } catch (HarnessException e) {
                                   throw new WingsException(e);
                                 }
                               })
                               .collect(Collectors.toList());
    }

    List<ExecutionStatus> conditions = yaml.getConditions()
                                           .stream()
                                           .map(condition -> Util.getEnumFromString(ExecutionStatus.class, condition))
                                           .collect(Collectors.toList());
    return NotificationRuleBuilder.aNotificationRule()
        .withActive(yaml.isActive())
        .withBatchNotifications(yaml.isBatchNotifications())
        .withConditions(conditions)
        .withExecutionScope(executionScope)
        .withNotificationGroups(notificationGroups)
        .build();
  }

  @Override
  public Yaml toYaml(NotificationRule bean, String appId) {
    List<String> conditionList =
        bean.getConditions().stream().map(condition -> condition.name()).collect(Collectors.toList());
    BaseYamlHandler notificationGroupYamlHandler =
        yamlHandlerFactory.getYamlHandler(YamlType.NOTIFICATION_GROUP, ObjectType.NOTIFICATION_GROUP);
    List<NotificationGroup.Yaml> notificationGroupYamlList =
        bean.getNotificationGroups()
            .stream()
            .map(notificationGroup
                -> (NotificationGroup.Yaml) notificationGroupYamlHandler.toYaml(notificationGroup, appId))
            .collect(Collectors.toList());

    return Yaml.Builder.anYaml()
        .withActive(bean.isActive())
        .withBatchNotifications(bean.isBatchNotifications())
        .withConditions(conditionList)
        .withExecutionScope(bean.getExecutionScope().name())
        .withNotificationGroups(notificationGroupYamlList)
        .build();
  }

  @Override
  public NotificationRule upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    return toBean(changeContext, changeSetContext);
  }

  @Override
  public boolean validate(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    return true;
  }

  @Override
  public Class getYamlClass() {
    return NotificationRule.Yaml.class;
  }

  @Override
  public NotificationRule get(String accountId, String yamlFilePath) {
    throw new WingsException(ErrorCode.UNSUPPORTED_OPERATION_EXCEPTION);
  }

  @Override
  public void delete(ChangeContext<Yaml> changeContext) throws HarnessException {
    // Do nothing
  }
}
