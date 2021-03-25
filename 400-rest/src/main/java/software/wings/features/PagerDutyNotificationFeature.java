package software.wings.features;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.HasPredicate.hasNone;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;

import software.wings.beans.security.UserGroup;
import software.wings.features.api.FeatureRestrictions;
import software.wings.features.api.Usage;
import software.wings.features.utils.NotificationUtils;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.WorkflowService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.stream.Collectors;

@OwnedBy(PL)
@Singleton
public class PagerDutyNotificationFeature extends AbstractNotificationFeature {
  public static final String FEATURE_NAME = "PAGERDUTY_NOTIFICATION";

  private final UserGroupService userGroupService;

  @Inject
  public PagerDutyNotificationFeature(AccountService accountService, FeatureRestrictions featureRestrictions,
      WorkflowService workflowService, UserGroupService userGroupService) {
    super(accountService, featureRestrictions, workflowService);
    this.userGroupService = userGroupService;
  }

  @Override
  public String getFeatureName() {
    return FEATURE_NAME;
  }

  // Get usages of PagerDuty under user groups
  @Override
  protected List<Usage> getUsages(String accountId) {
    return getUserGroups(accountId)
        .stream()
        .filter(PagerDutyNotificationFeature::hasPagerDuty)
        .map(NotificationUtils::asUsage)
        .collect(Collectors.toList());
  }

  private PageResponse<UserGroup> getUserGroups(String accountId) {
    return userGroupService.list(accountId, new PageRequest<>(), false);
  }

  private static boolean hasPagerDuty(UserGroup userGroup) {
    return userGroup.getNotificationSettings() != null
        && !hasNone(userGroup.getNotificationSettings().getPagerDutyIntegrationKey());
  }
}
