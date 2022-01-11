/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.functional.configAsCode;

import static io.harness.rule.OwnerRule.SHASWAT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.rule.Owner;
import io.harness.testframework.framework.git.GitData;
import io.harness.testframework.restutils.ConnectorUtils;
import io.harness.testframework.restutils.GitRestUtils;
import io.harness.testframework.restutils.SettingsUtils;

import software.wings.beans.Application;
import software.wings.beans.Environment;

import com.google.inject.Inject;
import java.util.List;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AppConfigAsCodeTest extends AbstractFunctionalTest {
  private static String CONNECTOR_GIT;
  private static String APPLICATION_NAME;
  private static String ENVIRONMENT_NAME;

  private static Application application = new Application();
  private static Environment environment = new Environment();
  private static String CONNECTOR_ID;
  private static String WEBHOOK_TOKEN;
  private static String ACCOUNT_ID;

  @Inject ConfigAsCodeHelper configAsCodeHelper;

  private void gitConnector() {
    ACCOUNT_ID = getAccount().getUuid();
    CONNECTOR_GIT = "Automation-GIT-Connector-" + System.currentTimeMillis();
    // Create Git Connector
    List<String> details = ConnectorUtils.createGitConnector(bearerToken, CONNECTOR_GIT, ACCOUNT_ID);
    CONNECTOR_ID = details.get(0);
    WEBHOOK_TOKEN = details.get(1);
  }

  private void deleteGitConnector() {
    SettingsUtils.delete(bearerToken, getAccount().getUuid(), CONNECTOR_ID);
  }

  @Test
  @Owner(developers = SHASWAT, intermittent = true)
  @Category(FunctionalTests.class)
  public void TC0_createAppWithGit() {
    gitConnector();

    APPLICATION_NAME = "Automation-App-" + System.currentTimeMillis();

    // Create an application with Git Sync
    application = configAsCodeHelper.createAppWithYaml(APPLICATION_NAME, CONNECTOR_ID, WEBHOOK_TOKEN, ACCOUNT_ID);

    // Validate the application in Git Repo
    List<GitData> gitDataList = GitRestUtils.getGitAllForApp(APPLICATION_NAME);

    int size = gitDataList.size();

    // Assert the size of list and 2 files created by default
    assertThat(size).isGreaterThanOrEqualTo(2);
    assertThat(gitDataList.get(size - 1).getName()).isEqualToIgnoringCase("Index.yaml");
    assertThat(gitDataList.get(size - 2).getName()).isEqualToIgnoringCase("Defaults.yaml");
    deleteGitConnector();
  }

  @Test
  @Owner(developers = SHASWAT, intermittent = true)
  @Category(FunctionalTests.class)
  public void TC1_updateAppWithGit() {
    gitConnector();

    // Update the GitSync in the application created
    String appName = "Automation-App-Update-YAML-" + System.currentTimeMillis();

    Application app = configAsCodeHelper.createApp(appName);
    configAsCodeHelper.createYamlGitConfig(ACCOUNT_ID, app.getAppId(), CONNECTOR_ID, bearerToken);

    // Validate the application in Git Repo
    List<GitData> gitDataList = GitRestUtils.getGitAllForApp(appName);

    int size = gitDataList.size();

    // Assert the size of list and 2 files created by default
    assertThat(size).isGreaterThanOrEqualTo(2);
    assertThat(gitDataList.get(size - 1).getName()).isEqualToIgnoringCase("Index.yaml");
    assertThat(gitDataList.get(size - 2).getName()).isEqualToIgnoringCase("Defaults.yaml");
    deleteGitConnector();
  }

  @Test
  @Owner(developers = SHASWAT, intermittent = true)
  @Category(FunctionalTests.class)
  public void TC2_addEnvInAppWithGit() {
    gitConnector();

    APPLICATION_NAME = "Automation-App-" + System.currentTimeMillis();
    ENVIRONMENT_NAME = "Automation-Env-" + System.currentTimeMillis();

    // Create an application with Git Sync
    application = configAsCodeHelper.createAppWithYaml(APPLICATION_NAME, CONNECTOR_ID, WEBHOOK_TOKEN, ACCOUNT_ID);

    // Add service in the application
    environment = configAsCodeHelper.createEnvironment(application, ENVIRONMENT_NAME);

    // Validate the Environment in Git Repo and 1 file created by default in Env folder
    List<GitData> gitEnvList = GitRestUtils.getGitEnvForApp(environment.getName(), application.getName());
    int size = gitEnvList.size();

    assertThat(size).isGreaterThanOrEqualTo(1);
    assertThat(gitEnvList.get(size - 1).getName()).isEqualToIgnoringCase("Index.yaml");
    deleteGitConnector();
  }
}
