package software.wings.service.impl.yaml.handler.notification;

import static software.wings.beans.NotificationGroup.NotificationGroupBuilder.aNotificationGroup;
import static software.wings.common.UUIDGenerator.generateUuid;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.commons.collections.CollectionUtils;
import software.wings.beans.ErrorCode;
import software.wings.beans.ExecutionScope;
import software.wings.beans.NotificationGroup;
import software.wings.beans.NotificationRule;
import software.wings.beans.NotificationRule.NotificationRuleBuilder;
import software.wings.beans.NotificationRule.Yaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.exception.WingsException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.intfc.NotificationSetupService;
import software.wings.sm.ExecutionStatus;
import software.wings.utils.Util;
import software.wings.utils.Validator;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author rktummala on 10/28/17
 */
@Singleton
public class NotificationRulesYamlHandler extends BaseYamlHandler<NotificationRule.Yaml, NotificationRule> {
  @Inject NotificationSetupService notificationSetupService;

  private NotificationRule toBean(ChangeContext<Yaml> changeContext, List<ChangeContext> changeContextList)
      throws HarnessException {
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();
    ExecutionScope executionScope = Util.getEnumFromString(ExecutionScope.class, yaml.getExecutionScope());

    List<NotificationGroup> notificationGroups = Lists.newArrayList();
    if (CollectionUtils.isNotEmpty(yaml.getNotificationGroups())) {
      notificationGroups =
          yaml.getNotificationGroups()
              .stream()
              .map(notificationGroupName -> {
                NotificationGroup notificationGroup =
                    notificationSetupService.readNotificationGroupByName(accountId, notificationGroupName);
                Validator.notNullCheck(
                    "Invalid notification group for the given name: " + notificationGroupName, notificationGroup);
                // We only store couple of fields in workflow when created from the normal ui.
                // Sticking to the same approach.
                return aNotificationGroup()
                    .withUuid(notificationGroup.getUuid())
                    .withEditable(notificationGroup.isEditable())
                    .build();
              })
              .collect(Collectors.toList());
    }

    List<ExecutionStatus> conditions = yaml.getConditions()
                                           .stream()
                                           .map(condition -> Util.getEnumFromString(ExecutionStatus.class, condition))
                                           .collect(Collectors.toList());
    return NotificationRuleBuilder.aNotificationRule()
        .withUuid(generateUuid())
        .withActive(true)
        .withBatchNotifications(false)
        .withConditions(conditions)
        .withExecutionScope(executionScope)
        .withNotificationGroups(notificationGroups)
        .build();
  }

  @Override
  public Yaml toYaml(NotificationRule bean, String appId) {
    List<String> conditionList =
        bean.getConditions().stream().map(condition -> condition.name()).collect(Collectors.toList());

    List<String> notificationGroupList =
        bean.getNotificationGroups()
            .stream()
            .map(notificationGroup -> {
              NotificationGroup notificationGroupFromDB = notificationSetupService.readNotificationGroup(
                  notificationGroup.getAccountId(), notificationGroup.getUuid());
              Validator.notNullCheck("Invalid notification group for the given id: " + notificationGroup.getUuid(),
                  notificationGroupFromDB);
              return notificationGroupFromDB.getName();
            })
            .collect(Collectors.toList());

    return Yaml.builder()
        .conditions(conditionList)
        .executionScope(Util.getStringFromEnum(bean.getExecutionScope()))
        .notificationGroups(notificationGroupList)
        .build();
  }

  @Override
  public NotificationRule upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    return toBean(changeContext, changeSetContext);
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
