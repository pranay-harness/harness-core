package migrations.all;

import com.google.common.base.Stopwatch;
import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import io.harness.persistence.HQuery;
import migrations.Migration;
import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class DeleteOrphanNotificationGroups implements Migration {
  private static final Logger log = LoggerFactory.getLogger(DeleteOrphanNotificationGroups.class);

  @Inject private WingsPersistence persistence;
  @Inject private UserGroupService userGroupService;
  @Inject private WorkflowService workflowService;

  @Override
  public void migrate() {
    log.info("Deleting zombie notification groups from workflows.");
    Stopwatch stopwatch = Stopwatch.createStarted();
    try {
      List<String> ngIds = persistence.createQuery(NotificationGroup.class, HQuery.excludeCount)
                               .filter("appId", Application.GLOBAL_APP_ID)
                               .asList()
                               .stream()
                               .map(NotificationGroup::getUuid)
                               .collect(Collectors.toList());

      Query<Workflow> query = persistence.createQuery(Workflow.class, HQuery.excludeAuthority);

      try (HIterator<Workflow> iterator = new HIterator<>(query.fetch())) {
        while (iterator.hasNext()) {
          Workflow workflow = iterator.next();
          workflow = workflowService.readWorkflow(workflow.getAppId(), workflow.getUuid());

          OrchestrationWorkflow owf = workflow.getOrchestrationWorkflow();
          String accountId = workflow.getAccountId();

          List<NotificationRule> notificationRules = owf.getNotificationRules()
                                                         .stream()
                                                         .peek(rule -> filterNotificationRule(rule, ngIds))
                                                         .peek(rule -> addDefaultUserGroup(rule, accountId))
                                                         .collect(Collectors.toList());

          owf.setNotificationRules(notificationRules);
          workflowService.updateWorkflow(workflow);
        }
      }

      log.info("Zombie notification groups deleted. Time Taken: {} ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));
    } catch (Exception e) {
      log.error("Error deleting zombie notification groups", e);
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
      log.error("No default UserGroup found for accountId={}", accountId);
      return;
    }

    rule.setUserGroupIds(Collections.singletonList(defaultUserGroup.getUuid()));
  }
}
