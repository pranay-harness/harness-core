/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.delegatetasks.validation;

import static io.harness.rule.OwnerRule.PRABU;

import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.DELEGATE_ID;
import static software.wings.utils.WingsTestConstants.PASSWORD;
import static software.wings.utils.WingsTestConstants.USER_NAME;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.TaskData;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.JiraConfig;
import software.wings.beans.TaskType;
import software.wings.delegatetasks.jira.JiraTask;

import java.util.ArrayList;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public class JiraTaskTest extends WingsBaseTest {
  @InjectMocks
  private JiraTask jiraTask =
      new JiraTask(DelegateTaskPackage.builder().delegateId(DELEGATE_ID).data(taskData).build(), null, null, null);

  static TaskData taskData = TaskData.builder().async(true).taskType(TaskType.JIRA.name()).build();

  private static String BASE_URL1 = "https://dummyjira.atlassian.net/";
  private static String BASE_URL2 = "https://dummyjira.atlassian.net/jira";
  private static String BASE_URL3 = "https://dummyjira.atlassian.net";

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void getIssueUrlTest() {
    JiraConfig jiraConfig =
        new JiraConfig(BASE_URL2, USER_NAME, PASSWORD, new String(PASSWORD), ACCOUNT_ID, new ArrayList<>());
    assertThat(jiraTask.getIssueUrl(jiraConfig, "TEST-1000"))
        .isEqualTo("https://dummyjira.atlassian.net/jira/browse/TEST-1000");
    jiraConfig = new JiraConfig(BASE_URL1, USER_NAME, PASSWORD, new String(PASSWORD), ACCOUNT_ID, new ArrayList<>());
    assertThat(jiraTask.getIssueUrl(jiraConfig, "TEST-1000"))
        .isEqualTo("https://dummyjira.atlassian.net/browse/TEST-1000");
    jiraConfig = new JiraConfig(BASE_URL3, USER_NAME, PASSWORD, new String(PASSWORD), ACCOUNT_ID, new ArrayList<>());
    assertThat(jiraTask.getIssueUrl(jiraConfig, "TEST-1000"))
        .isEqualTo("https://dummyjira.atlassian.net/browse/TEST-1000");
  }
}
