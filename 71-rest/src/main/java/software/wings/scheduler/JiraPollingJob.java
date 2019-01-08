package software.wings.scheduler;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.beans.EmbeddedUser;
import io.harness.beans.ExecutionStatus;
import io.harness.scheduler.PersistentScheduler;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import software.wings.beans.ApprovalDetails;
import software.wings.beans.approval.JiraApprovalParams;
import software.wings.service.impl.JiraHelperService;

public class JiraPollingJob implements Job {
  public static final String GROUP = "JIRA_POLLING_CRON_JOB";
  private static final int POLL_INTERVAL_SECONDS = 10;
  private static final String CONNECTOR_ID = "connectorId";
  private static final String ACCOUNT_ID = "accountId";
  private static final String APP_ID = "appId";
  private static final String ISSUE_ID = "issueId";
  private static final String APPROVAL_ID = "approvalId";
  private static final String WORKFLOW_EXECUTION_ID = "workflowExecutionId";

  @Inject private JiraHelperService jiraHelperService;
  @Inject @Named("BackgroundJobScheduler") private PersistentScheduler jobScheduler;

  public static void doPollingJob(PersistentScheduler jobScheduler, JiraApprovalParams jiraApprovalParams,
      String approvalExecutionId, String accountId, String appId, String workflowExecutionId) {
    jobScheduler.deleteJob(approvalExecutionId, GROUP);

    JobDetail job = JobBuilder.newJob(JiraPollingJob.class)
                        .withIdentity(approvalExecutionId, GROUP)
                        .usingJobData(CONNECTOR_ID, jiraApprovalParams.getJiraConnectorId())
                        .usingJobData(ACCOUNT_ID, accountId)
                        .usingJobData(APP_ID, appId)
                        .usingJobData(ISSUE_ID, jiraApprovalParams.getIssueId())
                        .usingJobData(APPROVAL_ID, approvalExecutionId)
                        .usingJobData("approvalField", jiraApprovalParams.getApprovalField())
                        .usingJobData("approvalValue", jiraApprovalParams.getApprovalValue())
                        .usingJobData("rejectionField", jiraApprovalParams.getRejectionField())
                        .usingJobData("rejectionValue", jiraApprovalParams.getRejectionValue())
                        .usingJobData(WORKFLOW_EXECUTION_ID, workflowExecutionId)
                        .build();

    Trigger trigger = TriggerBuilder.newTrigger()
                          .withIdentity(approvalExecutionId, GROUP)
                          .startNow()
                          .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                                            .withIntervalInSeconds(POLL_INTERVAL_SECONDS)
                                            .repeatForever()
                                            .withMisfireHandlingInstructionNowWithExistingCount())
                          .build();

    jobScheduler.scheduleJob(job, trigger);
  }

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    String connectorId = jobExecutionContext.getMergedJobDataMap().getString(CONNECTOR_ID);
    String accountId = jobExecutionContext.getMergedJobDataMap().getString(ACCOUNT_ID);
    String appId = jobExecutionContext.getMergedJobDataMap().getString(APP_ID);
    String approvalId = jobExecutionContext.getMergedJobDataMap().getString(APPROVAL_ID);
    String issueId = jobExecutionContext.getMergedJobDataMap().getString(ISSUE_ID);
    String approvalField = jobExecutionContext.getMergedJobDataMap().getString("approvalField");
    String approvalValue = jobExecutionContext.getMergedJobDataMap().getString("approvalValue");
    String rejectionField = jobExecutionContext.getMergedJobDataMap().getString("rejectionField");
    String rejectionValue = jobExecutionContext.getMergedJobDataMap().getString("rejectionValue");
    String workflowExecutionId = jobExecutionContext.getMergedJobDataMap().getString(WORKFLOW_EXECUTION_ID);

    boolean isTerminalState = false;

    try {
      ExecutionStatus approval = jiraHelperService.getApprovalStatus(
          connectorId, accountId, appId, issueId, approvalField, approvalValue, rejectionField, rejectionValue);
      // TODO:: Swagat: What if ticket not found or rejected. Also, there should be window till we should poll after
      // than it should delete

      if (approval == ExecutionStatus.SUCCESS || approval == ExecutionStatus.REJECTED) {
        isTerminalState = true;

        ApprovalDetails.Action action =
            approval == ExecutionStatus.SUCCESS ? ApprovalDetails.Action.APPROVE : ApprovalDetails.Action.REJECT;

        EmbeddedUser user = null;
        jiraHelperService.approveWorkflow(action, approvalId, user, appId, workflowExecutionId, approval);
      }
    } catch (Exception ex) {
      // TODO:: Swagat: Add logging and not all exceptions are terminal.
      isTerminalState = true;
    }

    if (isTerminalState) {
      jobScheduler.deleteJob(approvalId, GROUP);
    }
  }
}
