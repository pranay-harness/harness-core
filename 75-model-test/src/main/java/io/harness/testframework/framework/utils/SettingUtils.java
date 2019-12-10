package io.harness.testframework.framework.utils;

import static io.harness.generator.SettingGenerator.HARNESS_JIRA;
import static io.harness.generator.SettingGenerator.Settings.GITHUB_TEST_CONNECTOR;
import static io.harness.generator.SettingGenerator.Settings.TERRAFORM_CITY_GIT_REPO;
import static io.harness.generator.SettingGenerator.Settings.TERRAFORM_MAIN_GIT_REPO;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.Environment.GLOBAL_ENV_ID;
import static software.wings.beans.HostConnectionAttributes.AccessType.KEY;
import static software.wings.beans.HostConnectionAttributes.Builder.aHostConnectionAttributes;
import static software.wings.beans.HostConnectionAttributes.ConnectionType.SSH;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.SettingAttribute.SettingCategory.CONNECTOR;
import static software.wings.utils.UsageRestrictionsUtils.getAllAppAllEnvUsageRestrictions;

import software.wings.beans.GitConfig;
import software.wings.beans.JiraConfig;
import software.wings.beans.SettingAttribute;

public class SettingUtils {
  public static SettingAttribute createGitHubRepoSetting(String accountId, char[] key) {
    return aSettingAttribute()
        .withCategory(CONNECTOR)
        .withAccountId(accountId)
        .withAppId(GLOBAL_APP_ID)
        .withEnvId(GLOBAL_ENV_ID)
        .withName(GITHUB_TEST_CONNECTOR.name())
        .withValue(aHostConnectionAttributes()
                       .withConnectionType(SSH)
                       .withAccessType(KEY)
                       .withAccountId(accountId)
                       .withUserName("test-harness")
                       .withKey(key)
                       .build())
        .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
        .build();
  }

  public static SettingAttribute createTerraformCityGitRepoSetting(SettingAttribute githubKey, char[] password) {
    return aSettingAttribute()
        .withCategory(CONNECTOR)
        .withName(TERRAFORM_CITY_GIT_REPO.name())
        .withAppId(githubKey.getAppId())
        .withEnvId(githubKey.getEnvId())
        .withAccountId(githubKey.getAccountId())
        .withValue(GitConfig.builder()
                       .repoUrl("https://github.com/wings-software/terraform-test.git")
                       .username("test-harness")
                       .password(password)
                       .branch("master")
                       .accountId(githubKey.getAccountId())
                       .build())
        .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
        .build();
  }

  public static SettingAttribute createPCFFunctionalTestGitRepoSetting(SettingAttribute githubKey, char[] password) {
    return aSettingAttribute()
        .withCategory(CONNECTOR)
        .withName("pcf-functional-test")
        .withAppId(githubKey.getAppId())
        .withEnvId(githubKey.getEnvId())
        .withAccountId(githubKey.getAccountId())
        .withValue(GitConfig.builder()
                       .repoUrl("https://github.com/wings-software/pcf-functional-test.git")
                       .username("test-harness")
                       .password(password)
                       .branch("master")
                       .accountId(githubKey.getAccountId())
                       .build())
        .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
        .build();
  }

  public static SettingAttribute createTerraformMainGitRepoSetting(SettingAttribute githubKey, char[] password) {
    return aSettingAttribute()
        .withCategory(CONNECTOR)
        .withName(TERRAFORM_MAIN_GIT_REPO.name())
        .withAppId(githubKey.getAppId())
        .withEnvId(githubKey.getEnvId())
        .withAccountId(githubKey.getAccountId())
        .withValue(GitConfig.builder()
                       .repoUrl("https://github.com/wings-software/terraform.git")
                       .username("test-harness")
                       .password(password)
                       .branch("master")
                       .accountId(githubKey.getAccountId())
                       .build())
        .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
        .build();
  }

  public static SettingAttribute createHarnessJIRASetting(String accountId, char[] password) {
    return aSettingAttribute()
        .withName(HARNESS_JIRA)
        .withCategory(CONNECTOR)
        .withAccountId(accountId)
        .withValue(JiraConfig.builder()
                       .accountId(accountId)
                       .baseUrl("https://harness.atlassian.net")
                       .username("jirauser@harness.io")
                       .password(password)
                       .build())
        .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
        .build();
  }
}
