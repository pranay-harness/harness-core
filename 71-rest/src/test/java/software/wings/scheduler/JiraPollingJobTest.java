package software.wings.scheduler;

import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APPROVAL_EXECUTION_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.JIRA_CONNECTOR_ID;
import static software.wings.utils.WingsTestConstants.JIRA_ISSUE_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_EXECUTION_ID;

import com.google.inject.Inject;
import com.google.inject.Injector;

import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.scheduler.InjectorJobFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.approval.JiraApprovalParams;
import software.wings.rules.SetupScheduler;
import software.wings.service.impl.JiraHelperService;

import java.util.concurrent.TimeoutException;

@SetupScheduler
public class JiraPollingJobTest extends WingsBaseTest {
  private static final String APPROVAL_FIELD = "approvalField";
  private static final String APPROVAL_VALUE = "approvalValue";
  private static final String REJECTION_FIELD = "rejectionField";
  private static final String REJECTION_VALUE = "rejectionValue";

  @Inject private BackgroundJobScheduler jobScheduler;
  @Mock private Injector mockInjector;
  @InjectMocks private InjectorJobFactory injectorJobFactory = new InjectorJobFactory();
  @Mock private JiraHelperService jiraHelperService;
  @Inject @InjectMocks JiraPollingJob jiraPollingJob = new JiraPollingJob();

  private TestJobListener listener;

  @Before
  public void setUp() throws Exception {
    listener = new TestJobListener(JiraPollingJob.GROUP + "." + APPROVAL_EXECUTION_ID);
    jobScheduler.getScheduler().getListenerManager().addJobListener(listener);
    jobScheduler.getScheduler().setJobFactory(injectorJobFactory);
    when(mockInjector.getInstance(JiraPollingJob.class)).thenReturn(jiraPollingJob);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldDeleteJobOnSuccessfullApproval() throws TimeoutException, InterruptedException {
    when(jiraHelperService.getApprovalStatus(JIRA_CONNECTOR_ID, ACCOUNT_ID, APP_ID, JIRA_ISSUE_ID, APPROVAL_FIELD,
             APPROVAL_VALUE, REJECTION_FIELD, REJECTION_VALUE))
        .thenReturn(ExecutionStatus.SUCCESS);

    JiraApprovalParams jiraApprovalParams = new JiraApprovalParams();
    jiraApprovalParams.setJiraConnectorId(JIRA_CONNECTOR_ID);
    jiraApprovalParams.setIssueId(JIRA_ISSUE_ID);
    jiraApprovalParams.setApprovalField(APPROVAL_FIELD);
    jiraApprovalParams.setApprovalValue(APPROVAL_VALUE);
    jiraApprovalParams.setRejectionField(REJECTION_FIELD);
    jiraApprovalParams.setRejectionValue(REJECTION_VALUE);

    JiraPollingJob.doPollingJob(
        jobScheduler, jiraApprovalParams, APPROVAL_EXECUTION_ID, ACCOUNT_ID, APP_ID, WORKFLOW_EXECUTION_ID);
    listener.waitToSatisfy(ofSeconds(35));
    assertThat(jobScheduler.deleteJob(APPROVAL_EXECUTION_ID, JiraPollingJob.GROUP)).isFalse();
  }

  @Test
  @Category(UnitTests.class)
  public void shouldDeleteJobOnRejection() throws TimeoutException, InterruptedException {
    when(jiraHelperService.getApprovalStatus(JIRA_CONNECTOR_ID, ACCOUNT_ID, APP_ID, JIRA_ISSUE_ID, APPROVAL_FIELD,
             APPROVAL_VALUE, REJECTION_FIELD, REJECTION_VALUE))
        .thenReturn(ExecutionStatus.REJECTED);

    JiraApprovalParams jiraApprovalParams = new JiraApprovalParams();
    jiraApprovalParams.setJiraConnectorId(JIRA_CONNECTOR_ID);
    jiraApprovalParams.setIssueId(JIRA_ISSUE_ID);
    jiraApprovalParams.setApprovalField(APPROVAL_FIELD);
    jiraApprovalParams.setApprovalValue(APPROVAL_VALUE);
    jiraApprovalParams.setRejectionField(REJECTION_FIELD);
    jiraApprovalParams.setRejectionValue(REJECTION_VALUE);

    JiraPollingJob.doPollingJob(
        jobScheduler, jiraApprovalParams, APPROVAL_EXECUTION_ID, ACCOUNT_ID, APP_ID, WORKFLOW_EXECUTION_ID);
    listener.waitToSatisfy(ofSeconds(35));
    assertThat(jobScheduler.deleteJob(APPROVAL_EXECUTION_ID, JiraPollingJob.GROUP)).isFalse();
  }

  @Test
  @Category(UnitTests.class)
  public void shouldDeleteJobOnError() throws TimeoutException, InterruptedException {
    when(jiraHelperService.getApprovalStatus(JIRA_CONNECTOR_ID, ACCOUNT_ID, APP_ID, JIRA_ISSUE_ID, APPROVAL_FIELD,
             APPROVAL_VALUE, REJECTION_FIELD, REJECTION_VALUE))
        .thenThrow(new WingsException(ErrorCode.UNKNOWN_ERROR));

    JiraApprovalParams jiraApprovalParams = new JiraApprovalParams();
    jiraApprovalParams.setJiraConnectorId(JIRA_CONNECTOR_ID);
    jiraApprovalParams.setIssueId(JIRA_ISSUE_ID);
    jiraApprovalParams.setApprovalField(APPROVAL_FIELD);
    jiraApprovalParams.setApprovalValue(APPROVAL_VALUE);
    jiraApprovalParams.setRejectionField(REJECTION_FIELD);
    jiraApprovalParams.setRejectionValue(REJECTION_VALUE);

    JiraPollingJob.doPollingJob(
        jobScheduler, jiraApprovalParams, APPROVAL_EXECUTION_ID, ACCOUNT_ID, APP_ID, WORKFLOW_EXECUTION_ID);
    listener.waitToSatisfy(ofSeconds(35));
    assertThat(jobScheduler.deleteJob(APPROVAL_EXECUTION_ID, JiraPollingJob.GROUP)).isFalse();
  }

  @Test
  @Category(UnitTests.class)
  public void shouldNotDeleteJobOnAwitingApproval() throws TimeoutException, InterruptedException {
    when(jiraHelperService.getApprovalStatus(JIRA_CONNECTOR_ID, ACCOUNT_ID, APP_ID, JIRA_ISSUE_ID, APPROVAL_FIELD,
             APPROVAL_VALUE, REJECTION_FIELD, REJECTION_VALUE))
        .thenReturn(ExecutionStatus.FAILED);

    JiraApprovalParams jiraApprovalParams = new JiraApprovalParams();
    jiraApprovalParams.setJiraConnectorId(JIRA_CONNECTOR_ID);
    jiraApprovalParams.setIssueId(JIRA_ISSUE_ID);
    jiraApprovalParams.setApprovalField(APPROVAL_FIELD);
    jiraApprovalParams.setApprovalValue(APPROVAL_VALUE);
    jiraApprovalParams.setRejectionField(REJECTION_FIELD);
    jiraApprovalParams.setRejectionValue(REJECTION_VALUE);

    JiraPollingJob.doPollingJob(
        jobScheduler, jiraApprovalParams, APPROVAL_EXECUTION_ID, ACCOUNT_ID, APP_ID, WORKFLOW_EXECUTION_ID);
    listener.waitToSatisfy(ofSeconds(35));
    assertThat(jobScheduler.deleteJob(APPROVAL_EXECUTION_ID, JiraPollingJob.GROUP)).isTrue();
  }
}
