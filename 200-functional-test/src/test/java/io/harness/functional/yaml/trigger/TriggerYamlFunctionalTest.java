/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.functional.yaml.trigger;

import static io.harness.functional.yaml.YamlFunctionalTestConstants.BASE_CLONE_PATH;
import static io.harness.functional.yaml.YamlFunctionalTestConstants.BASE_GIT_REPO_PATH;
import static io.harness.functional.yaml.YamlFunctionalTestConstants.BASE_VERIFY_CLONE_PATH;
import static io.harness.functional.yaml.YamlFunctionalTestConstants.YAML_WEBHOOK_PAYLOAD_GITHUB;
import static io.harness.generator.AccountGenerator.ACCOUNT_ID;
import static io.harness.rule.OwnerRule.HARSH;

import static software.wings.beans.yaml.YamlConstants.APPLICATIONS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.SETUP_FOLDER_PATH;

import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.functional.yaml.YamlFunctionalTestHelper;
import io.harness.rule.Owner;

import software.wings.beans.Application;
import software.wings.beans.GitConfig;
import software.wings.beans.SettingAttribute;
import software.wings.service.intfc.AppService;
import software.wings.settings.SettingValue;

import com.google.inject.Inject;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class TriggerYamlFunctionalTest extends AbstractFunctionalTest {
  @Inject private AppService appService;
  @Inject YamlFunctionalTestHelper yamlFunctionalTestHelper;

  private SettingAttribute gitConnector;

  private String OLD_APP_NAME = "Trigger-App";
  private String NEW_APP_NAME = OLD_APP_NAME + "-1";
  private String CLOUD_PROVIDER_NAME;
  private String CLONE_PATH;
  private String GIT_CONNECTOR_NAME;
  private String REPO_NAME;
  private String GIT_REPO_PATH;
  private String VERIFY_CLONE_PATH;

  @Before
  public void setUpConfig() {
    REPO_NAME = this.getClass().getSimpleName();
    GIT_CONNECTOR_NAME = "GitConnector-" + REPO_NAME;
    CLOUD_PROVIDER_NAME = "CloudProvider-" + REPO_NAME;

    GIT_REPO_PATH = Paths.get(BASE_GIT_REPO_PATH, REPO_NAME).toString();
    VERIFY_CLONE_PATH = Paths.get(BASE_VERIFY_CLONE_PATH, REPO_NAME).toString();
    CLONE_PATH = Paths.get(BASE_CLONE_PATH, REPO_NAME).toString();

    cleanUpConfig();
    yamlFunctionalTestHelper.createLocalGitRepo(GIT_REPO_PATH);
    yamlFunctionalTestHelper.createGCPCloudProvider(CLOUD_PROVIDER_NAME);
    yamlFunctionalTestHelper.createGitConnector(GIT_CONNECTOR_NAME, GIT_REPO_PATH, bearerToken);
  }

  @Test
  @Owner(developers = HARSH)
  @Category(FunctionalTests.class)
  @Ignore("TODO: We are refactoring trigger and it does not require now")
  public void testTriggerYamlSupport() {
    Awaitility.await().atMost(120, TimeUnit.SECONDS).pollInterval(5, TimeUnit.SECONDS).until(() -> {
      yamlFunctionalTestHelper.uploadYamlZipFile(
          ACCOUNT_ID, OLD_APP_NAME, SETUP_FOLDER_PATH + APPLICATIONS_FOLDER, bearerToken);

      Application app = appService.getAppByName(ACCOUNT_ID, OLD_APP_NAME);

      yamlFunctionalTestHelper.createYamlGitConfig(ACCOUNT_ID, app.getUuid(), gitConnector.getUuid(), bearerToken);

      yamlFunctionalTestHelper.duplicateApplication(OLD_APP_NAME, NEW_APP_NAME, REPO_NAME, GIT_REPO_PATH, CLONE_PATH);

      SettingValue value = gitConnector.getValue();
      String webhookToken = ((GitConfig) value).getWebhookToken();
      yamlFunctionalTestHelper.triggerWebhookPost(ACCOUNT_ID, webhookToken, YAML_WEBHOOK_PAYLOAD_GITHUB, bearerToken);

      int mismatchCount = yamlFunctionalTestHelper.getMismatchCount(
          OLD_APP_NAME, NEW_APP_NAME, REPO_NAME, GIT_REPO_PATH, VERIFY_CLONE_PATH);
      return mismatchCount == 0;
    });
  }
  @After
  public void cleanUpConfig() {
    yamlFunctionalTestHelper.cleanUpApplication(OLD_APP_NAME, NEW_APP_NAME);
    yamlFunctionalTestHelper.deleteSettingAttributeByName(CLOUD_PROVIDER_NAME);
    yamlFunctionalTestHelper.deleteSettingAttributeByName(GIT_CONNECTOR_NAME);

    yamlFunctionalTestHelper.cleanUpRepoFolders(REPO_NAME, GIT_REPO_PATH, CLONE_PATH, VERIFY_CLONE_PATH);
  }
}
