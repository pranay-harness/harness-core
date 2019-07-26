package software.wings.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.utils.WingsTestConstants.USER_ID;
import static software.wings.utils.WingsTestConstants.USER_NAME;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.category.element.UnitTests;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.LicenseInfo;
import software.wings.beans.User;
import software.wings.beans.governance.GovernanceConfig;
import software.wings.features.GovernanceFeature;
import software.wings.features.api.PremiumFeature;
import software.wings.licensing.LicenseService;
import software.wings.security.UserRequestContext;
import software.wings.security.UserThreadLocal;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.compliance.GovernanceConfigService;
import software.wings.utils.WingsTestConstants;

/**
 *
 * @author rktummala
 */
public class GovernanceConfigServiceTest extends WingsBaseTest {
  @Inject private AccountService accountService;
  @Inject private LicenseService licenseService;
  @Inject private GovernanceConfigService governanceConfigService;
  @Inject @Named(GovernanceFeature.FEATURE_NAME) private PremiumFeature governanceFeature;

  private String accountId;

  /**
   * Sets mocks.
   */
  @Before
  public void setUp() {
    Account account = anAccount()
                          .withAccountName(WingsTestConstants.ACCOUNT_NAME)
                          .withCompanyName(WingsTestConstants.COMPANY_NAME)
                          .withLicenseInfo(getLicenseInfo())
                          .build();
    accountService.save(account);
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
  @Category(UnitTests.class)
  public void testUpdateAndRead() {
    GovernanceConfig defaultConfig = GovernanceConfig.builder().accountId(accountId).deploymentFreeze(false).build();
    GovernanceConfig governanceConfig = governanceConfigService.get(accountId);
    compare(defaultConfig, governanceConfig);

    GovernanceConfig inputConfig = GovernanceConfig.builder().accountId(accountId).deploymentFreeze(true).build();
    GovernanceConfig savedGovernanceConfig = governanceConfigService.update(accountId, inputConfig);
    compare(inputConfig, savedGovernanceConfig);

    savedGovernanceConfig = governanceConfigService.get(accountId);
    compare(inputConfig, savedGovernanceConfig);

    inputConfig = GovernanceConfig.builder().accountId(accountId).deploymentFreeze(false).build();
    savedGovernanceConfig = governanceConfigService.update(accountId, inputConfig);
    compare(inputConfig, savedGovernanceConfig);

    savedGovernanceConfig = governanceConfigService.get(accountId);
    compare(inputConfig, savedGovernanceConfig);

    inputConfig = GovernanceConfig.builder().accountId(accountId).deploymentFreeze(true).build();

    for (String restrictedAccountType : governanceFeature.getRestrictedAccountTypes()) {
      LicenseInfo newLicenseInfo = getLicenseInfo();
      newLicenseInfo.setAccountType(restrictedAccountType);
      licenseService.updateAccountLicense(accountId, newLicenseInfo);
      try {
        governanceConfigService.update(accountId, inputConfig);
        fail("Saved governance config");
      } catch (WingsException e) {
        assertEquals(ErrorCode.INVALID_REQUEST, e.getCode());
      }
    }
  }

  private void compare(GovernanceConfig lhs, GovernanceConfig rhs) {
    assertEquals(lhs.getAccountId(), rhs.getAccountId());
    assertEquals(lhs.isDeploymentFreeze(), rhs.isDeploymentFreeze());
  }
}
