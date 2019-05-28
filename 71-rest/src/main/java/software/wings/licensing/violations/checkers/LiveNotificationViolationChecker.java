package software.wings.licensing.violations.checkers;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.beans.Base.ACCOUNT_ID_KEY;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.data.structure.CollectionUtils;
import io.harness.data.structure.EmptyPredicate;
import software.wings.beans.EntityType;
import software.wings.beans.FeatureUsageViolation;
import software.wings.beans.FeatureUsageViolation.Usage;
import software.wings.beans.FeatureViolation;
import software.wings.beans.GraphNode;
import software.wings.beans.Workflow;
import software.wings.beans.security.UserGroup;
import software.wings.licensing.violations.FeatureViolationChecker;
import software.wings.licensing.violations.RestrictedFeature;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.StateType;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

@Singleton
public class LiveNotificationViolationChecker implements FeatureViolationChecker, WorkflowViolationCheckerMixin {
  private UserGroupService userGroupService;
  private WorkflowService workflowService;

  @Inject
  public LiveNotificationViolationChecker(UserGroupService userGroupService, WorkflowService workflowService) {
    this.userGroupService = userGroupService;
    this.workflowService = workflowService;
  }

  public static final Predicate<GraphNode> IS_JIRA_STATE_PRESENT =
      gn -> Objects.equals(StateType.JIRA_CREATE_UPDATE.name(), gn.getType());

  private static final Predicate<GraphNode> IS_SNOW_STATE_PRESENT =
      gn -> Objects.equals(StateType.SERVICENOW_CREATE_UPDATE.name(), gn.getType());

  @Override
  public List<FeatureViolation> getViolationsForCommunityAccount(String accountId) {
    List<FeatureViolation> featureViolationList = null;
    List<Usage> usages = new ImmutableList.Builder<Usage>()
                             .addAll(getSlackUsages(accountId))
                             .addAll(getJiraUsages(accountId))
                             .addAll(getSnowUsages(accountId))
                             .addAll(getPagerDutyUsages(accountId))
                             .build();
    if (isNotEmpty(usages)) {
      featureViolationList =
          Collections.singletonList(new FeatureUsageViolation(RestrictedFeature.LIVE_NOTIFICATIONS, usages));
    }

    return CollectionUtils.emptyIfNull(featureViolationList);
  }

  // Get usages of PagerDuty under user groups
  private Iterable<Usage> getPagerDutyUsages(String accountId) {
    return getUserGroups(accountId)
        .stream()
        .filter(LiveNotificationViolationChecker::hasPagerDuty)
        .map(LiveNotificationViolationChecker::asUsage)
        .collect(Collectors.toList());
  }

  // Get usages of Slack under user groups
  private List<Usage> getSlackUsages(String accountId) {
    return getUserGroups(accountId)
        .stream()
        .filter(LiveNotificationViolationChecker::hasSlack)
        .map(LiveNotificationViolationChecker::asUsage)
        .collect(Collectors.toList());
  }

  private static Usage asUsage(UserGroup userGroup) {
    return Usage.builder()
        .entityId(userGroup.getUuid())
        .entityType(EntityType.USER_GROUP.toString())
        .entityName(userGroup.getName())
        .build();
  }

  private PageResponse<UserGroup> getUserGroups(String accountId) {
    return userGroupService.list(accountId, new PageRequest<>(), false);
  }

  // gets usages of JIRA state under workflows
  private List<Usage> getJiraUsages(String accountId) {
    return getWorkflowViolationUsages(getAllWorkflowsByAccountId(accountId), IS_JIRA_STATE_PRESENT);
  }

  // gets usages of Service Now state under workflows
  private List<Usage> getSnowUsages(String accountId) {
    return getWorkflowViolationUsages(getAllWorkflowsByAccountId(accountId), IS_SNOW_STATE_PRESENT);
  }

  private static boolean hasSlack(UserGroup userGroup) {
    return userGroup.getNotificationSettings() != null && userGroup.getNotificationSettings().getSlackConfig() != null
        && !EmptyPredicate.isEmpty(userGroup.getNotificationSettings().getSlackConfig().getOutgoingWebhookUrl());
  }

  private static boolean hasPagerDuty(UserGroup userGroup) {
    return userGroup.getNotificationSettings() != null
        && !EmptyPredicate.isEmpty(userGroup.getNotificationSettings().getPagerDutyIntegrationKey());
  }

  private List<Workflow> getAllWorkflowsByAccountId(@NotNull String accountId) {
    PageRequest<Workflow> pageRequest =
        aPageRequest().withLimit(PageRequest.UNLIMITED).addFilter(ACCOUNT_ID_KEY, Operator.EQ, accountId).build();

    return workflowService.listWorkflows(pageRequest).getResponse();
  }
}
