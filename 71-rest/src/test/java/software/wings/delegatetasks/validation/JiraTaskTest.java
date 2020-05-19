package software.wings.delegatetasks.validation;

import static io.harness.rule.OwnerRule.PRABU;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.DELEGATE_ID;
import static software.wings.utils.WingsTestConstants.PASSWORD;
import static software.wings.utils.WingsTestConstants.USER_NAME;

import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskData;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import software.wings.WingsBaseTest;
import software.wings.beans.DelegateTaskPackage;
import software.wings.beans.JiraConfig;
import software.wings.beans.TaskType;
import software.wings.delegatetasks.jira.JiraTask;

public class JiraTaskTest extends WingsBaseTest {
  @InjectMocks
  private JiraTask jiraTask = new JiraTask(
      DelegateTaskPackage.builder().delegateId(DELEGATE_ID).delegateTask(delegateTask).build(), null, null);
  static DelegateTask delegateTask = DelegateTask.builder()
                                         .uuid("id")
                                         .accountId(ACCOUNT_ID)
                                         .appId(APP_ID)
                                         .waitId("")
                                         .data(TaskData.builder().async(true).taskType(TaskType.JIRA.name()).build())
                                         .build();

  private static String BASE_URL1 = "https://dummyjira.atlassian.net/";
  private static String BASE_URL2 = "https://dummyjira.atlassian.net/jira";
  private static String BASE_URL3 = "https://dummyjira.atlassian.net";

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void getIssueUrlTest() {
    JiraConfig jiraConfig = new JiraConfig(BASE_URL2, USER_NAME, PASSWORD, new String(PASSWORD), ACCOUNT_ID);
    assertThat(jiraTask.getIssueUrl(jiraConfig, "TEST-1000"))
        .isEqualTo("https://dummyjira.atlassian.net/jira/browse/TEST-1000");
    jiraConfig = new JiraConfig(BASE_URL1, USER_NAME, PASSWORD, new String(PASSWORD), ACCOUNT_ID);
    assertThat(jiraTask.getIssueUrl(jiraConfig, "TEST-1000"))
        .isEqualTo("https://dummyjira.atlassian.net/browse/TEST-1000");
    jiraConfig = new JiraConfig(BASE_URL3, USER_NAME, PASSWORD, new String(PASSWORD), ACCOUNT_ID);
    assertThat(jiraTask.getIssueUrl(jiraConfig, "TEST-1000"))
        .isEqualTo("https://dummyjira.atlassian.net/browse/TEST-1000");
  }
}
