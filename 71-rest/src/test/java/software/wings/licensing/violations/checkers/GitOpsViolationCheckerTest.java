package software.wings.licensing.violations.checkers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.wings.WingsBaseTest;
import software.wings.beans.FeatureUsageLimitExceededViolation;
import software.wings.beans.FeatureViolation;
import software.wings.beans.GitConfig;
import software.wings.beans.SettingAttribute;
import software.wings.licensing.violations.RestrictedFeature;
import software.wings.service.impl.SettingValidationService;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.SettingsService;

import java.util.List;

/**
 * @author Vaibhav Tulsyan
 * 08/May/2019
 */
public class GitOpsViolationCheckerTest extends WingsBaseTest {
  @Mock private AccountService accountService;
  @Mock private SettingValidationService settingValidationService;

  @InjectMocks @Inject private SettingsService settingsService;
  @Inject GitOpsViolationChecker gitOpsViolationChecker;

  private static final int MAX_GIT_CONNECTORS_ALLOWED_IN_TRIAL = Integer.MAX_VALUE;
  private static final int MAX_GIT_CONNECTORS_ALLOWED_IN_COMMUNITY = 1;

  private GitConfig createGitConfig(String accountId, String repoUrl) {
    return GitConfig.builder()
        .accountId(accountId)
        .repoUrl(repoUrl)
        .username("someUsername")
        .password("somePassword".toCharArray())
        .build();
  }

  @Test
  @Category(UnitTests.class)
  public void getViolationsForCommunityAccount() {
    String accountId = "someAccountId";
    when(accountService.isCommunityAccount(accountId)).thenReturn(false);
    when(settingValidationService.validate(Mockito.any(SettingAttribute.class))).thenReturn(true);

    final int GIT_CONNECTOR_COUNT = 2; // save 2 Git Connectors
    for (int i = 0; i < GIT_CONNECTOR_COUNT; i++) {
      GitConfig gitConfig = createGitConfig(accountId, "https://github.com/someOrg/someRepo" + i + ".git");
      settingsService.save(
          aSettingAttribute().withName("Git Connector " + i).withAccountId(accountId).withValue(gitConfig).build());
    }
    List<FeatureViolation> violations = gitOpsViolationChecker.getViolationsForCommunityAccount(accountId);
    assertEquals(1, violations.size());

    FeatureViolation violation = violations.get(0);
    assertEquals(FeatureViolation.Category.RESTRICTED_FEATURE_USAGE_LIMIT_EXCEEDED, violation.getViolationCategory());
    assertEquals(RestrictedFeature.GIT_OPS, violation.getRestrictedFeature());
    assertEquals(MAX_GIT_CONNECTORS_ALLOWED_IN_TRIAL,
        ((FeatureUsageLimitExceededViolation) violation).getPaidLicenseUsageLimit());
    assertEquals(
        MAX_GIT_CONNECTORS_ALLOWED_IN_COMMUNITY, ((FeatureUsageLimitExceededViolation) violation).getUsageLimit());
    assertEquals(GIT_CONNECTOR_COUNT, ((FeatureUsageLimitExceededViolation) violation).getUsageCount());
  }
}