package io.harness.delegate.task.jira.connection;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.rule.OwnerRule.ALEXEI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.connector.jira.JiraConnectionTaskParams;
import io.harness.delegate.beans.connector.jira.connection.JiraTestConnectionTaskNGResponse;
import io.harness.delegate.task.jira.JiraTaskNGHelper;
import io.harness.delegate.task.jira.JiraTaskNGResponse;
import io.harness.rule.Owner;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(CDC)
public class JiraTestConnectionTaskNGTest extends CategoryTest {
  @Mock private JiraTaskNGHelper jiraTaskNGHelper;
  @InjectMocks
  private final JiraTestConnectionTaskNG jiraTestConnectionTaskNG = new JiraTestConnectionTaskNG(
      DelegateTaskPackage.builder().data(TaskData.builder().build()).build(), null, null, null);

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void testRunObjectParamsShouldThrowMotImplementedException() {
    assertThatThrownBy(() -> jiraTestConnectionTaskNG.run(new Object[1]))
        .hasMessage("This method is deprecated")
        .isInstanceOf(NotImplementedException.class);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void testRun() {
    JiraTaskNGResponse taskResponse = JiraTaskNGResponse.builder().build();
    when(jiraTaskNGHelper.getJiraTaskResponse(any())).thenReturn(taskResponse);
    DelegateResponseData response = jiraTestConnectionTaskNG.run(JiraConnectionTaskParams.builder().build());

    assertThat(((JiraTestConnectionTaskNGResponse) response).getCanConnect()).isEqualTo(true);

    verify(jiraTaskNGHelper).getJiraTaskResponse(any());
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void testRunWhenCantConnect() {
    when(jiraTaskNGHelper.getJiraTaskResponse(any())).thenThrow(new RuntimeException("exception"));
    DelegateResponseData response = jiraTestConnectionTaskNG.run(JiraConnectionTaskParams.builder().build());

    assertThat(((JiraTestConnectionTaskNGResponse) response).getCanConnect()).isEqualTo(false);

    verify(jiraTaskNGHelper).getJiraTaskResponse(any());
  }
}
