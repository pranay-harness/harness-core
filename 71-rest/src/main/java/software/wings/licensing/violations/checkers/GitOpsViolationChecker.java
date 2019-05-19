package software.wings.licensing.violations.checkers;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.beans.SearchFilter.Operator;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.AccountType;
import software.wings.beans.FeatureUsageLimitExceededViolation;
import software.wings.beans.FeatureViolation;
import software.wings.beans.SettingAttribute;
import software.wings.licensing.violations.FeatureViolationChecker;
import software.wings.licensing.violations.RestrictedFeature;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;

/**
 * @author Vaibhav Tulsyan
 * 30/Apr/2019
 */
@Slf4j
@Singleton
public class GitOpsViolationChecker implements FeatureViolationChecker {
  private SettingsService settingsService;

  private static final int MAX_GIT_CONNECTORS_ALLOWED_IN_PAID = Integer.MAX_VALUE;
  private static final int MAX_GIT_CONNECTORS_ALLOWED_IN_TRIAL = Integer.MAX_VALUE;
  private static final int MAX_GIT_CONNECTORS_ALLOWED_IN_COMMUNITY = 1;

  private static final Map<String, Integer> maxGitConnectorsAllowed = new HashMap<String, Integer>() {
    {
      put(AccountType.TRIAL, MAX_GIT_CONNECTORS_ALLOWED_IN_TRIAL);
      put(AccountType.COMMUNITY, MAX_GIT_CONNECTORS_ALLOWED_IN_COMMUNITY);
      put(AccountType.PAID, MAX_GIT_CONNECTORS_ALLOWED_IN_PAID);
    }
  };

  @Inject
  public GitOpsViolationChecker(SettingsService settingsService) {
    this.settingsService = settingsService;
  }

  @Override
  public List<FeatureViolation> getViolationsForCommunityAccount(@NotNull String accountId) {
    PageRequest<SettingAttribute> request =
        aPageRequest()
            .addFilter(SettingAttribute.ACCOUNT_ID_KEY, Operator.EQ, accountId)
            .addFilter(SettingAttribute.VALUE_TYPE_KEY, Operator.EQ, SettingVariableTypes.GIT)
            .build();
    int currentGitConnectorCount = settingsService.list(request, null, null).getResponse().size();

    int allowedNumberOfGitConnectors = maxGitConnectorsAllowed.get(AccountType.COMMUNITY);

    if (currentGitConnectorCount > allowedNumberOfGitConnectors) {
      logger.info(
          "GitOps usage limit exceeded - Account ID: {}, Current Git Connector Usage: {}, Allowed Git Connector Usage: {}",
          accountId, currentGitConnectorCount, allowedNumberOfGitConnectors);
      return Collections.singletonList(FeatureUsageLimitExceededViolation.builder()
                                           .restrictedFeature(RestrictedFeature.GIT_OPS)
                                           .paidLicenseUsageLimit(MAX_GIT_CONNECTORS_ALLOWED_IN_PAID)
                                           .usageCount(currentGitConnectorCount)
                                           .usageLimit(allowedNumberOfGitConnectors)
                                           .build());
    }

    return Collections.emptyList();
  }
}
