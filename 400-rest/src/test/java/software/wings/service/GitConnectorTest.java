/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service;

import static io.harness.eraro.ErrorCode.USAGE_LIMITS_EXCEEDED;
import static io.harness.rule.OwnerRule.ANKIT;

import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.exception.WingsException;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.GitConfig;
import software.wings.beans.SettingAttribute;
import software.wings.features.api.UsageLimitedFeature;
import software.wings.service.impl.GitConfigHelperService;
import software.wings.service.impl.SettingValidationService;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.SettingsService;

import com.google.inject.Inject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

/**
 * @author Vaibhav Tulsyan
 * 07/May/2019
 */
public class GitConnectorTest extends WingsBaseTest {
  @Inject private HPersistence persistence;
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
  @Owner(developers = ANKIT)
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
  @Owner(developers = ANKIT)
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
      assertThat(USAGE_LIMITS_EXCEEDED).isEqualTo(e.getCode());
      failed = true;
    }
    assertThat(failed).isTrue();
  }
}
