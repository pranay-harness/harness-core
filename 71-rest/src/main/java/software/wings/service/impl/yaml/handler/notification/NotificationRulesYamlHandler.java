package software.wings.service.impl.yaml.handler.notification;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.exception.WingsException.USER;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.NotificationGroup.NotificationGroupBuilder.aNotificationGroup;
import static software.wings.utils.Validator.notNullCheck;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.ExecutionStatus;
import io.harness.eraro.ErrorCode;
import io.harness.exception.HarnessException;
import io.harness.exception.WingsException;
import software.wings.beans.ExecutionScope;
import software.wings.beans.NotificationGroup;
import software.wings.beans.NotificationRule;
import software.wings.beans.NotificationRule.NotificationRuleBuilder;
import software.wings.beans.NotificationRule.Yaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.intfc.NotificationSetupService;
import software.wings.utils.Util;

import java.util.List;

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

    if (isNotEmpty(yaml.getNotificationGroups())) {
      notificationGroups =
          yaml.getNotificationGroups()
              .stream()
              .map(notificationGroupName -> {
                if (yaml.isNotificationGroupAsExpression()) {
                  return aNotificationGroup().withName(notificationGroupName).build();
                }
                NotificationGroup notificationGroup =
                    notificationSetupService.readNotificationGroupByName(accountId, notificationGroupName);
                notNullCheck(
                    "Invalid notification group for the given name: " + notificationGroupName, notificationGroup, USER);
                // We only store couple of fields in workflow when created from the normal ui.
                // Sticking to the same approach.
                return aNotificationGroup()
                    .withUuid(notificationGroup.getUuid())
                    .withEditable(notificationGroup.isEditable())
                    .build();
              })
              .collect(toList());
    }

    List<ExecutionStatus> conditions = yaml.getConditions()
                                           .stream()
                                           .map(condition -> Util.getEnumFromString(ExecutionStatus.class, condition))
                                           .collect(toList());
    return NotificationRuleBuilder.aNotificationRule()
        .withUuid(generateUuid())
        .withActive(true)
        .withBatchNotifications(false)
        .withConditions(conditions)
        .withExecutionScope(executionScope)
        .withNotificationGroupAsExpression(yaml.isNotificationGroupAsExpression())
        .withUserGroupAsExpression(yaml.isUserGroupAsExpression())
        .withUserGroupExpression(yaml.getUserGroupExpression())
        .withUserGroupIds(yaml.getUserGroupIds())
        .withNotificationGroups(notificationGroups)
        .build();
  }

  @Override
  public Yaml toYaml(NotificationRule notificationRule, String appId) {
    List<String> conditionList =
        notificationRule.getConditions().stream().map(condition -> condition.name()).collect(toList());

    List<String> notificationGroupList =
        notificationRule.getNotificationGroups()
            .stream()
            .map(notificationGroup -> {
              if (!notificationRule.isNotificationGroupAsExpression()) {
                NotificationGroup notificationGroupFromDB = notificationSetupService.readNotificationGroup(
                    notificationGroup.getAccountId(), notificationGroup.getUuid());
                notNullCheck("Invalid notification group for the given id: " + notificationGroup.getUuid(),
                    notificationGroupFromDB, USER);
                return notificationGroupFromDB.getName();
              }
              return notificationGroup.getName();
            })
            .collect(toList());

    return Yaml.builder()
        .conditions(conditionList)
        .executionScope(Util.getStringFromEnum(notificationRule.getExecutionScope()))
        .notificationGroups(notificationGroupList)
        .notificationGroupAsExpression(notificationRule.isNotificationGroupAsExpression())
        .userGroupIds(notificationRule.getUserGroupIds())
        .userGroupAsExpression(notificationRule.isUserGroupAsExpression())
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
