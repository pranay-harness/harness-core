/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.functional.migration;

import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.security.UserGroup.DEFAULT_ACCOUNT_ADMIN_USER_GROUP_NAME;

import static io.github.benas.randombeans.api.EnhancedRandom.random;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EnvironmentType;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.functional.WorkflowUtils;
import io.harness.generator.AccountGenerator;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import io.harness.testframework.framework.Setup;
import io.harness.testframework.framework.utils.JiraUtils;
import io.harness.testframework.framework.utils.SettingUtils;
import io.harness.testframework.restutils.AccountRestUtils;
import io.harness.testframework.restutils.ApiKeysRestUtils;
import io.harness.testframework.restutils.ApplicationRestUtils;
import io.harness.testframework.restutils.EnvironmentRestUtils;
import io.harness.testframework.restutils.IPWhitelistingRestUtils;
import io.harness.testframework.restutils.SettingsUtils;
import io.harness.testframework.restutils.WorkflowRestUtils;

import software.wings.beans.Account;
import software.wings.beans.AccountType;
import software.wings.beans.ApiKeyEntry;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.LicenseInfo;
import software.wings.beans.LicenseUpdateInfo;
import software.wings.beans.SettingAttribute;
import software.wings.beans.User;
import software.wings.beans.Workflow;
import software.wings.beans.security.UserGroup;
import software.wings.beans.security.access.Whitelist;
import software.wings.service.intfc.SettingsService;

import com.google.inject.Inject;
import java.util.Arrays;
import java.util.Map;
import org.junit.Before;

@OwnedBy(HarnessTeam.PL)
public abstract class AbstractAccountMigrationTestBase extends AbstractFunctionalTest {
  private static final String testUserPassword = "testPassword";

  @Inject private AccountGenerator accountGenerator;
  @Inject private SettingsService settingsService;
  @Inject private ScmSecret scmSecret;

  private Account account;
  private User user;
  private Application app;
  private Environment env;

  @Before
  public void setUp() {
    account = createTrialAccount();
    user = createUser();
    addUserToAdminUserGroup();
    addUserToHarnessUserGroup();
    loginUser();
    app = createApplication();
    env = createEnvironment();
  }

  private Account createTrialAccount() {
    return accountGenerator.ensureAccount(random(String.class), random(String.class), AccountType.TRIAL);
  }

  private User createUser() {
    return accountGenerator.ensureUser(
        random(String.class), random(String.class), generateRandomEmail(), testUserPassword.toCharArray(), account);
  }

  private void addUserToAdminUserGroup() {
    accountGenerator.addUserToUserGroup(user, account.getUuid(), UserGroup.DEFAULT_ACCOUNT_ADMIN_USER_GROUP_NAME);
  }

  private void addUserToHarnessUserGroup() {
    accountGenerator.addUserToHarnessUserGroup(account.getUuid(), user);
  }

  private void loginUser() {
    user = Setup.loginUser(user.getEmail(), testUserPassword);
  }

  private String generateRandomEmail() {
    return random(String.class).toLowerCase() + "@harness.io";
  }

  private Application createApplication() {
    Application application = anApplication().name(random(String.class)).build();
    return ApplicationRestUtils.createApplication(user.getToken(), account, application);
  }

  private Environment createEnvironment() {
    Environment environment =
        anEnvironment().name(random(String.class)).environmentType(EnvironmentType.NON_PROD).build();
    return EnvironmentRestUtils.createEnvironment(user.getToken(), account, app.getAppId(), environment);
  }

  public final String getAccountType() {
    return AccountRestUtils.getAccount(account.getUuid(), user.getToken()).getLicenseInfo().getAccountType();
  }

  public final void updateAccountLicense(String accountType) {
    updateAccountLicense(accountType, null);
  }

  public final void updateAccountLicense(String accountType, Map<String, Map<String, Object>> requiredInfoToComply) {
    LicenseInfo licenseInfo = LicenseInfo.builder().accountType(accountType).build();
    LicenseUpdateInfo licenseUpdateInfo =
        LicenseUpdateInfo.builder().licenseInfo(licenseInfo).requiredInfoToComply(requiredInfoToComply).build();

    AccountRestUtils.updateAccountLicense(account.getUuid(), user.getToken(), licenseUpdateInfo);
  }

  public void addSourceRepos() {
    char[] key = scmSecret.decryptToCharArray(new SecretName("playground_private_key"));
    SettingAttribute gitHubRepo = SettingUtils.createGitHubRepoSetting(account.getUuid(), key);
    SettingsUtils.create(user.getToken(), account.getUuid(), gitHubRepo);

    char[] terraformPassword = scmSecret.decryptToCharArray(new SecretName("terraform_password"));
    settingsService.forceSave(SettingUtils.createTerraformCityGitRepoSetting(gitHubRepo, terraformPassword));
    settingsService.forceSave(SettingUtils.createTerraformMainGitRepoSetting(gitHubRepo, terraformPassword));
  }

  public void addWhitelistedIP() {
    Whitelist whitelist = Whitelist.builder().accountId(account.getUuid()).filter("255.255.255.255").build();
    IPWhitelistingRestUtils.addWhiteListing(account.getUuid(), user.getToken(), whitelist);
  }

  public void addApiKey() {
    ApiKeyEntry apiKeyEntry = ApiKeyEntry.builder()
                                  .name(random(String.class))
                                  .userGroupIds(Arrays.asList(DEFAULT_ACCOUNT_ADMIN_USER_GROUP_NAME))
                                  .accountId(account.getUuid())
                                  .build();
    ApiKeysRestUtils.createApiKey(account.getUuid(), user.getToken(), apiKeyEntry);
  }

  public void addWorkflowWithJira() {
    SettingAttribute jiraConnector = addJIRAConnector();
    Workflow workflow = WorkflowUtils.buildCanaryWorkflowPostDeploymentStep(
        "WF", env.getUuid(), JiraUtils.getJiraCreateNodeWithoutCustomFields(jiraConnector.getUuid()));
    WorkflowRestUtils.createWorkflow(user.getToken(), account.getUuid(), app.getAppId(), workflow);
  }

  private SettingAttribute addJIRAConnector() {
    char[] jiraPassword = scmSecret.decryptToCharArray(new SecretName("harness_jira"));

    SettingAttribute harnessJIRA = SettingUtils.createHarnessJIRASetting(account.getUuid(), jiraPassword);
    return settingsService.forceSave(harnessJIRA);
  }
}
