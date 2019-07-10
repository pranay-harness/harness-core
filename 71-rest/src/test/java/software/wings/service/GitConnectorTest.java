package software.wings.service;

import static io.harness.eraro.ErrorCode.USAGE_LIMITS_EXCEEDED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.exception.WingsException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.wings.WingsBaseTest;
import software.wings.beans.GitConfig;
import software.wings.beans.SettingAttribute;
import software.wings.dl.WingsPersistence;
import software.wings.features.api.UsageLimitedFeature;
import software.wings.service.impl.GitConfigHelperService;
import software.wings.service.impl.SettingValidationService;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.SettingsService;

/**
 * @author Vaibhav Tulsyan
 * 07/May/2019
 */
public class GitConnectorTest extends WingsBaseTest {
  @Inject private WingsPersistence wingsPersistence;
  @Mock private AccountService accountService;
  @Mock private GitConfigHelperService gitConfigHelperService;
  @Mock private SettingValidationService settingValidationService;
  @Mock private UsageLimitedFeature gitOpsFeature;

  @InjectMocks @Inject private SettingsService settingsService;

  @Rule public ExpectedException exceptionRule = ExpectedException.none();

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
  public void test_creationOfGitConnectorsWithinLimitInHarnessCommunity_shouldPass() {
    String accountId = "someAccountId";
    int maxGitConnectorsAllowed = 1;
    when(gitOpsFeature.getMaxUsageAllowedForAccount(accountId)).thenReturn(maxGitConnectorsAllowed);

    when(settingValidationService.validate(Mockito.any(SettingAttribute.class))).thenReturn(true);

    for (int i = 0; i < maxGitConnectorsAllowed; i++) {
      GitConfig gitConfig = createGitConfig(accountId, "https://github.com/someOrg/someRepo" + i + ".git");
      // This save should pass
      settingsService.save(
          aSettingAttribute().withName("Git Connector " + i).withAccountId(accountId).withValue(gitConfig).build());
    }
  }

  @Test
  @Category(UnitTests.class)
  public void test_creationOfGitConnectorsAboveLimitInHarnessCommunity_shouldFail() {
    String accountId = "someAccountId";
    int maxGitConnectorsAllowed = 1;
    when(gitOpsFeature.getMaxUsageAllowedForAccount(accountId)).thenReturn(maxGitConnectorsAllowed);

    for (int i = 0; i < maxGitConnectorsAllowed; i++) {
      GitConfig gitConfig = createGitConfig(accountId, "https://github.com/someOrg/someRepo" + i + ".git");
      // This save should pass
      settingsService.save(
          aSettingAttribute().withName("Git Connector " + i).withAccountId(accountId).withValue(gitConfig).build());
    }

    boolean failed = false;
    try {
      GitConfig gitConfig =
          createGitConfig(accountId, "https://github.com/someOrg/someRepo" + maxGitConnectorsAllowed + ".git");
      // This save should throw WingsException
      settingsService.save(aSettingAttribute()
                               .withName("Git Connector " + maxGitConnectorsAllowed)
                               .withAccountId(accountId)
                               .withValue(gitConfig)
                               .build());
    } catch (WingsException e) {
      assertEquals(e.getCode(), USAGE_LIMITS_EXCEEDED);
      failed = true;
    }
    assertTrue(failed);
  }
}
