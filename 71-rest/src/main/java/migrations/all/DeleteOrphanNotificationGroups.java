package migrations.all;

import static io.harness.persistence.HQuery.excludeAuthority;

import com.google.common.base.Stopwatch;
import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import io.harness.persistence.HQuery;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import org.mongodb.morphia.query.Query;
import software.wings.beans.Application;
import software.wings.beans.NotificationGroup;
import software.wings.beans.NotificationRule;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.Workflow;
import software.wings.beans.security.UserGroup;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.WorkflowService;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Delete Notification Groups in workflows which are not in `notificationGroups` collection
 */
@Slf4j
public class DeleteOrphanNotificationGroups implements Migration {
  @Inject private WingsPersistence persistence;
  @Inject private UserGroupService userGroupService;
  @Inject private WorkflowService workflowService;

  @Override
  public void migrate() {
    logger.info("Deleting zombie notification groups from workflows.");
    Stopwatch stopwatch = Stopwatch.createStarted();
    try {
      List<String> ngIds = persistence.createQuery(NotificationGroup.class, HQuery.excludeCount)
                               .filter("appId", Application.GLOBAL_APP_ID)
                               .asList()
                               .stream()
                               .map(NotificationGroup::getUuid)
                               .collect(Collectors.toList());

      Query<Workflow> query = persistence.createQuery(Workflow.class, excludeAuthority);

      try (HIterator<Workflow> iterator = new HIterator<>(query.fetch())) {
        for (Workflow workflow : iterator) {
          workflow = workflowService.readWorkflow(workflow.getAppId(), workflow.getUuid());

          OrchestrationWorkflow owf = workflow.getOrchestrationWorkflow();
          String accountId = workflow.getAccountId();

          List<NotificationRule> notificationRules = owf.getNotificationRules()
                                                         .stream()
                                                         .peek(rule -> filterNotificationRule(rule, ngIds))
                                                         .peek(rule -> addDefaultUserGroup(rule, accountId))
                                                         .collect(Collectors.toList());

          owf.setNotificationRules(notificationRules);
          workflowService.updateWorkflow(workflow, false);
        }
      }

      logger.info("Zombie notification groups deleted. Time Taken: {} ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));
    } catch (Exception e) {
      logger.error("Error deleting zombie notification groups", e);
    }
  }

  private void filterNotificationRule(NotificationRule rule, List<String> notificationGroupIds) {
    List<NotificationGroup> filteredGroups = rule.getNotificationGroups()
                                                 .stream()
                                                 .filter(ng -> notificationGroupIds.contains(ng.getUuid()))
                                                 .collect(Collectors.toList());

    rule.setNotificationGroups(filteredGroups);
  }

  private void addDefaultUserGroup(NotificationRule rule, String accountId) {
    UserGroup defaultUserGroup = userGroupService.getDefaultUserGroup(accountId);
    if (null == defaultUserGroup) {
      logger.error("No default UserGroup found for accountId={}", accountId);
      return;
    }

    rule.setUserGroupIds(Collections.singletonList(defaultUserGroup.getUuid()));
  }
}
