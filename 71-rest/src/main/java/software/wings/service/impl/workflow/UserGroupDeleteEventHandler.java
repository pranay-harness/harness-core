package software.wings.service.impl.workflow;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.beans.SearchFilter.Operator;
import io.harness.data.structure.CollectionUtils;
import software.wings.beans.NotificationRule;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowKeys;
import software.wings.service.intfc.WorkflowService;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Singleton
public class UserGroupDeleteEventHandler {
  @Inject private WorkflowService workflowService;

  /**
   * When a user group os deleted, delete corresponding groups from workflow notification strategy
   */
  public void handleUserGroupDelete(String accountId, String deletedUserGroupId) {
    updateNotificationStrategyOnUserGroupDelete(accountId, deletedUserGroupId);
  }

  private void updateNotificationStrategyOnUserGroupDelete(String accountId, String deletedUserGroupId) {
    PageRequest<Workflow> request =
        (PageRequest<Workflow>) aPageRequest()
            .addFilter(WorkflowKeys.accountId, Operator.EQ, accountId)
            .addFilter("orchestration.notificationRules.userGroupIds", Operator.CONTAINS, deletedUserGroupId)
            .build();

    List<Workflow> workflows = workflowService.listWorkflows(request);
    for (Workflow workflow : workflows) {
      modifyWorkflow(workflow, deletedUserGroupId);
      workflowService.updateWorkflow(workflow, false);
    }
  }

  private static void modifyWorkflow(Workflow workflow, String deletedUserGroupId) {
    List<NotificationRule> rules =
        CollectionUtils.emptyIfNull(workflow.getOrchestrationWorkflow().getNotificationRules());

    List<NotificationRule> newRules = rules.stream()
                                          .map(rule -> mapRule(rule, deletedUserGroupId))
                                          .filter(Optional::isPresent)
                                          .map(Optional::get)
                                          .collect(Collectors.toList());

    workflow.getOrchestrationWorkflow().setNotificationRules(newRules);
  }

  private static Optional<NotificationRule> mapRule(NotificationRule rule, String deletedUserGroupId) {
    List<String> filteredUgIds =
        rule.getUserGroupIds().stream().filter(ugId -> !deletedUserGroupId.equals(ugId)).collect(Collectors.toList());

    rule.setUserGroupIds(filteredUgIds);
    if (!filteredUgIds.isEmpty()) {
      return Optional.of(rule);
    } else {
      return Optional.empty();
    }
  }
}
