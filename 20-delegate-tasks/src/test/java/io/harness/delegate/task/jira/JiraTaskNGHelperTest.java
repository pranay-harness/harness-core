package io.harness.delegate.task.jira;

import static io.harness.rule.OwnerRule.ALEXEI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.delegate.task.jira.response.JiraTaskNGResponse;
import io.harness.jira.JiraAction;
import io.harness.logging.CommandExecutionStatus;
import io.harness.rule.Owner;
import io.harness.security.encryption.SecretDecryptionService;

import java.util.ArrayList;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class JiraTaskNGHelperTest {
  @Mock private JiraTaskNGHandler jiraTaskNGHandler;
  @Mock private SecretDecryptionService secretDecryptionService;

  @InjectMocks private JiraTaskNGHelper jiraTaskNGHelper;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestGetJiraTaskResponseAuth() {
    JiraTaskNGResponse mockedResponse =
        JiraTaskNGResponse.builder().executionStatus(CommandExecutionStatus.SUCCESS).build();
    JiraTaskNGParameters jiraTaskNGParameters = JiraTaskNGParameters.builder().jiraAction(JiraAction.AUTH).build();
    when(secretDecryptionService.decrypt(any(), any())).thenReturn(null);
    when(jiraTaskNGHandler.validateCredentials(jiraTaskNGParameters)).thenReturn(mockedResponse);

    JiraTaskNGResponse jiraTaskResponse = jiraTaskNGHelper.getJiraTaskResponse(jiraTaskNGParameters);

    assertThat(jiraTaskResponse).isNotNull();
    assertThat(jiraTaskResponse.getExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestGetJiraTaskResponseCreateTicket() {
    final String issueId = "issueID";
    final String issueType = "Bug";
    JiraTaskNGResponse mockedResponse =
        JiraTaskNGResponse.builder().executionStatus(CommandExecutionStatus.SUCCESS).build();
    JiraTaskNGParameters jiraTaskNGParameters = JiraTaskNGParameters.builder()
                                                    .jiraAction(JiraAction.CREATE_TICKET)
                                                    .issueId(issueId)
                                                    .issueType(issueType)
                                                    .build();
    when(secretDecryptionService.decrypt(any(), any())).thenReturn(null);
    when(secretDecryptionService.decrypt(any(), any())).thenReturn(null);
    when(jiraTaskNGHandler.createTicket(jiraTaskNGParameters)).thenReturn(mockedResponse);

    JiraTaskNGResponse jiraTaskResponse = jiraTaskNGHelper.getJiraTaskResponse(jiraTaskNGParameters);

    assertThat(jiraTaskResponse).isNotNull();
    assertThat(jiraTaskResponse.getExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestGetJiraTaskResponseUpdateTicket() {
    final String issueId = "issueID";
    final String issueType = "Bug";
    JiraTaskNGResponse mockedResponse =
        JiraTaskNGResponse.builder().executionStatus(CommandExecutionStatus.SUCCESS).build();
    JiraTaskNGParameters jiraTaskNGParameters = JiraTaskNGParameters.builder()
                                                    .jiraAction(JiraAction.UPDATE_TICKET)
                                                    .updateIssueIds(Collections.singletonList(issueId))
                                                    .issueType(issueType)
                                                    .build();
    when(secretDecryptionService.decrypt(any(), any())).thenReturn(null);
    when(secretDecryptionService.decrypt(any(), any())).thenReturn(null);
    when(jiraTaskNGHandler.updateTicket(jiraTaskNGParameters)).thenReturn(mockedResponse);

    JiraTaskNGResponse jiraTaskResponse = jiraTaskNGHelper.getJiraTaskResponse(jiraTaskNGParameters);

    assertThat(jiraTaskResponse).isNotNull();
    assertThat(jiraTaskResponse.getExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestGetJiraTaskResponseFetchIssue() {
    JiraTaskNGResponse mockedResponse =
        JiraTaskNGResponse.builder().executionStatus(CommandExecutionStatus.RUNNING).build();
    JiraTaskNGParameters jiraTaskNGParameters =
        JiraTaskNGParameters.builder().jiraAction(JiraAction.FETCH_ISSUE).build();
    when(secretDecryptionService.decrypt(any(), any())).thenReturn(null);
    when(jiraTaskNGHandler.fetchIssue(jiraTaskNGParameters)).thenReturn(mockedResponse);

    JiraTaskNGResponse jiraTaskResponse = jiraTaskNGHelper.getJiraTaskResponse(jiraTaskNGParameters);

    assertThat(jiraTaskResponse).isNotNull();
    assertThat(jiraTaskResponse.getExecutionStatus()).isEqualTo(CommandExecutionStatus.RUNNING);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestGetJiraTaskResponseGetProjects() {
    final String issueId = "issueID";
    final String issueType = "Bug";
    JiraTaskNGResponse mockedResponse = JiraTaskNGResponse.builder()
                                            .executionStatus(CommandExecutionStatus.SUCCESS)
                                            .projects(new ArrayList<>())
                                            .build();
    JiraTaskNGParameters jiraTaskNGParameters = JiraTaskNGParameters.builder()
                                                    .jiraAction(JiraAction.GET_PROJECTS)
                                                    .issueId(issueId)
                                                    .issueType(issueType)
                                                    .build();
    when(secretDecryptionService.decrypt(any(), any())).thenReturn(null);
    when(secretDecryptionService.decrypt(any(), any())).thenReturn(null);
    when(jiraTaskNGHandler.getProjects(jiraTaskNGParameters)).thenReturn(mockedResponse);

    JiraTaskNGResponse jiraTaskResponse = jiraTaskNGHelper.getJiraTaskResponse(jiraTaskNGParameters);

    assertThat(jiraTaskResponse).isNotNull();
    assertThat(jiraTaskResponse.getExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    assertThat(jiraTaskResponse.getProjects()).isNotNull();
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestGetJiraTaskResponseGetStatuses() {
    JiraTaskNGResponse mockedResponse =
        JiraTaskNGResponse.builder().executionStatus(CommandExecutionStatus.SUCCESS).build();
    JiraTaskNGParameters jiraTaskNGParameters =
        JiraTaskNGParameters.builder().jiraAction(JiraAction.GET_STATUSES).build();
    when(secretDecryptionService.decrypt(any(), any())).thenReturn(null);
    when(jiraTaskNGHandler.getStatuses(jiraTaskNGParameters)).thenReturn(mockedResponse);

    JiraTaskNGResponse jiraTaskResponse = jiraTaskNGHelper.getJiraTaskResponse(jiraTaskNGParameters);

    assertThat(jiraTaskResponse).isNotNull();
    assertThat(jiraTaskResponse.getExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestGetJiraTaskResponseGetFieldsAndOptions() {
    JiraTaskNGResponse mockedResponse =
        JiraTaskNGResponse.builder().executionStatus(CommandExecutionStatus.SUCCESS).build();
    when(secretDecryptionService.decrypt(any(), any())).thenReturn(null);
    JiraTaskNGParameters jiraTaskNGParameters =
        JiraTaskNGParameters.builder().jiraAction(JiraAction.GET_FIELDS_OPTIONS).build();

    when(secretDecryptionService.decrypt(any(), any())).thenReturn(null);
    when(secretDecryptionService.decrypt(any(), any())).thenReturn(null);
    when(jiraTaskNGHandler.getFieldsOptions(jiraTaskNGParameters)).thenReturn(mockedResponse);

    JiraTaskNGResponse jiraTaskResponse = jiraTaskNGHelper.getJiraTaskResponse(jiraTaskNGParameters);

    assertThat(jiraTaskResponse).isNotNull();
    assertThat(jiraTaskResponse.getExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestGetJiraTaskResponseCheckApproval() {
    final String issueId = "issueID";
    JiraTaskNGResponse mockedResponse = JiraTaskNGResponse.builder()
                                            .executionStatus(CommandExecutionStatus.SUCCESS)
                                            .projects(new ArrayList<>())
                                            .build();
    JiraTaskNGParameters jiraTaskNGParameters = JiraTaskNGParameters.builder()
                                                    .jiraAction(JiraAction.CHECK_APPROVAL)
                                                    .approvalField("status")
                                                    .approvalValue("Success")
                                                    .issueId(issueId)
                                                    .build();
    when(secretDecryptionService.decrypt(any(), any())).thenReturn(null);
    when(secretDecryptionService.decrypt(any(), any())).thenReturn(null);
    when(jiraTaskNGHandler.checkJiraApproval(jiraTaskNGParameters)).thenReturn(mockedResponse);

    JiraTaskNGResponse jiraTaskResponse = jiraTaskNGHelper.getJiraTaskResponse(jiraTaskNGParameters);

    assertThat(jiraTaskResponse).isNotNull();
    assertThat(jiraTaskResponse.getExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    assertThat(jiraTaskResponse.getProjects()).isNotNull();
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestGetJiraTaskResponseGetCreateMetadata() {
    JiraTaskNGResponse mockedResponse =
        JiraTaskNGResponse.builder().executionStatus(CommandExecutionStatus.SUCCESS).build();
    when(secretDecryptionService.decrypt(any(), any())).thenReturn(null);
    JiraTaskNGParameters jiraTaskNGParameters =
        JiraTaskNGParameters.builder().jiraAction(JiraAction.GET_CREATE_METADATA).build();

    when(secretDecryptionService.decrypt(any(), any())).thenReturn(null);
    when(secretDecryptionService.decrypt(any(), any())).thenReturn(null);
    when(jiraTaskNGHandler.getCreateMetadata(jiraTaskNGParameters)).thenReturn(mockedResponse);

    JiraTaskNGResponse jiraTaskResponse = jiraTaskNGHelper.getJiraTaskResponse(jiraTaskNGParameters);

    assertThat(jiraTaskResponse).isNotNull();
    assertThat(jiraTaskResponse.getExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
  }
}
