/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.functional.migration;

import static io.harness.rule.OwnerRule.ANKIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.FunctionalTests;
import io.harness.generator.SettingGenerator.Settings;
import io.harness.rule.Owner;

import software.wings.beans.AccountType;
import software.wings.features.GitOpsFeature;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class AccountMigrationToCommunityTest extends AbstractAccountMigrationTestBase {
  @Owner(developers = ANKIT)
  @Category(FunctionalTests.class)
  public void testMigrateAlreadyCompliantTrialAccountToCommunity() {
    updateAccountLicense(AccountType.COMMUNITY);

    assertThat(getAccountType()).isEqualTo(AccountType.COMMUNITY);
  }

  @Test
  @Owner(developers = ANKIT)
  @Category(FunctionalTests.class)
  public void testMigrateNonCompliantTrialAccountToCommunity() {
    makeAccountNonCompliant();

    updateAccountLicense(AccountType.COMMUNITY);

    assertThat(AccountType.COMMUNITY).isNotEqualTo(getAccountType());

    Map<String, Object> gitOpsComplyInfo = new HashMap<>();
    gitOpsComplyInfo.put("sourceReposToRetain", Arrays.asList(Settings.TERRAFORM_MAIN_GIT_REPO.name()));

    Map<String, Map<String, Object>> requiredInfoToComply = new HashMap<>();
    requiredInfoToComply.put(GitOpsFeature.FEATURE_NAME, gitOpsComplyInfo);

    updateAccountLicense(AccountType.COMMUNITY, requiredInfoToComply);

    assertThat(getAccountType()).isEqualTo(AccountType.COMMUNITY);
  }

  private void makeAccountNonCompliant() {
    addSourceRepos();

    addWhitelistedIP();
    addApiKey();

    addWorkflowWithJira();
  }
}
