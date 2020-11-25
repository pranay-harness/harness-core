package io.harness.delegate.task.jira;

import static io.harness.rule.OwnerRule.ALEXEI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.jira.response.JiraTaskNGResponse;
import io.harness.logging.CommandExecutionStatus;
import io.harness.rule.Owner;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class JiraTaskNGTest extends CategoryTest {
  @Mock private JiraTaskNGHelper jiraTaskNGHelper;
  @InjectMocks
  private final JiraTaskNG jiraTaskNG =
      new JiraTaskNG(DelegateTaskPackage.builder().data(TaskData.builder().build()).build(), null, null, null);

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void testRunObjectParamsShouldThrowMotImplementedException() {
    assertThatThrownBy(() -> jiraTaskNG.run(new Object[1]))
        .hasMessage("not implemented")
        .isInstanceOf(NotImplementedException.class);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void testRun() {
    JiraTaskNGResponse taskResponse =
        JiraTaskNGResponse.builder().executionStatus(CommandExecutionStatus.SUCCESS).build();
    when(jiraTaskNGHelper.getJiraTaskResponse(any())).thenReturn(taskResponse);
    DelegateResponseData response = jiraTaskNG.run(JiraTaskNGParameters.builder().build());

    assertThat(((JiraTaskNGResponse) response).getExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);

    verify(jiraTaskNGHelper).getJiraTaskResponse(JiraTaskNGParameters.builder().build());
  }
}
