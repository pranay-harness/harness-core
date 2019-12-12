package software.wings.service;

import static io.harness.rule.OwnerRule.RAMA;
import static io.harness.rule.OwnerRule.VARDAN_BANSAL;
import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.utils.WingsTestConstants.USER_ID;
import static software.wings.utils.WingsTestConstants.USER_NAME;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.category.element.IntegrationTests;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.governance.TimeRangeBasedFreezeConfig;
import io.harness.governance.WeeklyFreezeConfig;
import io.harness.rule.OwnerRule.Owner;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Account;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.LicenseInfo;
import software.wings.beans.User;
import software.wings.beans.governance.GovernanceConfig;
import software.wings.features.GovernanceFeature;
import software.wings.features.api.PremiumFeature;
import software.wings.integration.BaseIntegrationTest;
import software.wings.licensing.LicenseService;
import software.wings.resources.stats.model.TimeRange;
import software.wings.resources.stats.model.WeeklyRange;
import software.wings.security.UserRequestContext;
import software.wings.security.UserThreadLocal;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.compliance.GovernanceConfigService;
import software.wings.utils.WingsTestConstants;

import java.util.Collections;

/**
 *
 * @author rktummala
 */
public class GovernanceConfigServiceTest extends BaseIntegrationTest {
  @Inject private AccountService accountService;
  @Inject private LicenseService licenseService;
  @Inject private GovernanceConfigService governanceConfigService;
  @Inject @Named(GovernanceFeature.FEATURE_NAME) private PremiumFeature governanceFeature;

  private String accountId = "some-account-uuid-" + RandomStringUtils.randomAlphanumeric(5);

  /**
   * Sets mocks.
   */
  @Before
  public void setUp() {
    Account account = anAccount()
                          .withUuid(accountId)
                          .withAccountName(WingsTestConstants.ACCOUNT_NAME)
                          .withCompanyName(WingsTestConstants.COMPANY_NAME)
                          .withLicenseInfo(getLicenseInfo())
                          .build();
    accountService.save(account, false);
    accountId = account.getUuid();
    setUserRequestContext();
  }

  private void setUserRequestContext() {
    User user = User.Builder.anUser().withName(USER_NAME).withUuid(USER_ID).build();
    user.setUserRequestContext(UserRequestContext.builder().accountId(accountId).build());
    UserThreadLocal.set(user);
  }

  /**
   * Should save and read.
   *
   */
  @Test
  @Owner(developers = RAMA)
  @Category(IntegrationTests.class)
  public void testUpdateAndRead() {
    GovernanceConfig defaultConfig = GovernanceConfig.builder().accountId(accountId).deploymentFreeze(false).build();
    GovernanceConfig governanceConfig = governanceConfigService.get(accountId);
    compare(defaultConfig, governanceConfig);

    GovernanceConfig inputConfig = GovernanceConfig.builder().accountId(accountId).deploymentFreeze(true).build();
    GovernanceConfig savedGovernanceConfig = governanceConfigService.upsert(accountId, inputConfig);
    compare(inputConfig, savedGovernanceConfig);

    savedGovernanceConfig = governanceConfigService.get(accountId);
    compare(inputConfig, savedGovernanceConfig);

    TimeRange range = new TimeRange(100L, 200L, "Asia/Kolkata");
    WeeklyRange weeklyRange = new WeeklyRange(null, "Tuesday", "7:00 PM", "Monday", "5:00 AM", "Asia/Kolkata");
    TimeRangeBasedFreezeConfig timeRangeBasedFreezeConfig = new TimeRangeBasedFreezeConfig(
        true, Collections.emptyList(), Collections.singletonList(EnvironmentType.PROD), range);
    WeeklyFreezeConfig weeklyFreezeConfig = new WeeklyFreezeConfig(
        true, Collections.emptyList(), Collections.singletonList(EnvironmentType.PROD), weeklyRange);

    inputConfig = GovernanceConfig.builder()
                      .accountId(accountId)
                      .deploymentFreeze(false)
                      .timeRangeBasedFreezeConfigs(Collections.singletonList(timeRangeBasedFreezeConfig))
                      .weeklyFreezeConfigs(Collections.singletonList(weeklyFreezeConfig))
                      .build();

    savedGovernanceConfig = governanceConfigService.upsert(accountId, inputConfig);
    compare(inputConfig, savedGovernanceConfig);

    savedGovernanceConfig = governanceConfigService.get(accountId);
    compare(inputConfig, savedGovernanceConfig);

    inputConfig = GovernanceConfig.builder().accountId(accountId).deploymentFreeze(true).build();

    for (String restrictedAccountType : governanceFeature.getRestrictedAccountTypes()) {
      LicenseInfo newLicenseInfo = getLicenseInfo();
      newLicenseInfo.setAccountType(restrictedAccountType);
      licenseService.updateAccountLicense(accountId, newLicenseInfo);
      try {
        governanceConfigService.upsert(accountId, inputConfig);
        fail("Saved governance config");
      } catch (WingsException e) {
        assertThat(e.getCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
      }
    }
  }

  /**
   * Should not be able to add a weekly window with invalid range
   */
  @Test
  @Owner(developers = VARDAN_BANSAL)
  @Category(IntegrationTests.class)
  public void createGovernanceConfig_shouldThrowException() {
    try {
      new WeeklyRange(null, "Monday", "7:00 PM", "Tuesday", "5:00 AM", "Asia/Kolkata");
    } catch (Exception ex) {
      assertTrue(ex instanceof IllegalArgumentException);
    }
  }

  private void compare(GovernanceConfig lhs, GovernanceConfig rhs) {
    assertThat(rhs.getAccountId()).isEqualTo(lhs.getAccountId());
    assertThat(rhs.isDeploymentFreeze()).isEqualTo(lhs.isDeploymentFreeze());
  }
}
