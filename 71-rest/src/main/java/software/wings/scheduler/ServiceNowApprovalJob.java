package software.wings.scheduler;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.beans.EmbeddedUser;
import io.harness.eraro.ErrorCode;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.WingsException;
import io.harness.scheduler.PersistentScheduler;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import software.wings.beans.ApprovalDetails;
import software.wings.beans.approval.ServiceNowApprovalParams;
import software.wings.service.intfc.servicenow.ServiceNowService;

import java.util.Calendar;
import java.util.Date;

@Slf4j
public class ServiceNowApprovalJob implements Job {
  public static final String GROUP = "SERVICE_NOW_POLLING_CRON_JOB";
  private static final int POLL_INTERVAL_SECONDS = 60;
  private static final int DELAY_START_SECONDS = 30;
  private static final String CONNECTOR_ID = "connectorId";
  private static final String ISSUE_NUMBER = "issueNumber";
  private static final String APPROVAL_ID = "approvalId";
  private static final String TICKET_TYPE = "ticketType";
  private static final String ACCOUNT_ID_KEY = "accountId";
  private static final String APP_ID_KEY = "appId";
  private static final String APPROVAL_FIELD = "approvalField";
  private static final String APPROVAL_VALUE = "approvalValue";
  private static final String REJECTION_FIELD = "rejectionField";
  private static final String REJECTION_VALUE = "rejectionValue";
  private static final String WORKFLOW_EXECUTION_ID = "workflowExecutionId";

  @Inject private ServiceNowService serviceNowService;
  @Inject @Named("BackgroundJobScheduler") private PersistentScheduler jobScheduler;

  public static void doPollingJob(PersistentScheduler jobScheduler, ServiceNowApprovalParams servicenowApprovalParams,
      String approvalExecutionId, String accountId, String appId, String workflowExecutionId, String ticketType) {
    if (servicenowApprovalParams.getSnowConnectorId() == null) {
      throw new WingsException(ErrorCode.SERVICENOW_ERROR, WingsException.USER)
          .addParam("message", "Service now ConnectorId cannot be null");
    }
    JobDetail job = JobBuilder.newJob(ServiceNowApprovalJob.class)
                        .withIdentity(approvalExecutionId, GROUP)
                        .usingJobData(CONNECTOR_ID, servicenowApprovalParams.getSnowConnectorId())
                        .usingJobData(ACCOUNT_ID_KEY, accountId)
                        .usingJobData(APP_ID_KEY, appId)
                        .usingJobData(TICKET_TYPE, ticketType)
                        .usingJobData(ISSUE_NUMBER, servicenowApprovalParams.getIssueNumber())
                        .usingJobData(APPROVAL_ID, approvalExecutionId)
                        .usingJobData(APPROVAL_FIELD, servicenowApprovalParams.getApprovalField())
                        .usingJobData(APPROVAL_VALUE, servicenowApprovalParams.getApprovalValue())
                        .usingJobData(REJECTION_FIELD, servicenowApprovalParams.getRejectionField())
                        .usingJobData(REJECTION_VALUE, servicenowApprovalParams.getRejectionValue())
                        .usingJobData(WORKFLOW_EXECUTION_ID, workflowExecutionId)
                        .build();

    scheduleJob(jobScheduler, approvalExecutionId, job, GROUP);
  }

  public static void scheduleJob(
      PersistentScheduler jobScheduler, String approvalExecutionId, JobDetail job, String group) {
    Calendar cal = Calendar.getInstance();
    cal.add(Calendar.SECOND, DELAY_START_SECONDS);
    Date delay_start = cal.getTime();

    Trigger trigger = TriggerBuilder.newTrigger()
                          .withIdentity(approvalExecutionId, group)
                          .startAt(delay_start)
                          .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                                            .withIntervalInSeconds(POLL_INTERVAL_SECONDS)
                                            .repeatForever()
                                            .withMisfireHandlingInstructionNowWithExistingCount())
                          .build();

    jobScheduler.ensureJob__UnderConstruction(job, trigger);
  }

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    String connectorId = jobExecutionContext.getMergedJobDataMap().getString(CONNECTOR_ID);
    String accountId = jobExecutionContext.getMergedJobDataMap().getString(ACCOUNT_ID_KEY);
    String appId = jobExecutionContext.getMergedJobDataMap().getString(APP_ID_KEY);
    String approvalId = jobExecutionContext.getMergedJobDataMap().getString(APPROVAL_ID);
    String issueNumber = jobExecutionContext.getMergedJobDataMap().getString(ISSUE_NUMBER);
    String approvalField = jobExecutionContext.getMergedJobDataMap().getString(APPROVAL_FIELD);
    String approvalValue = jobExecutionContext.getMergedJobDataMap().getString(APPROVAL_VALUE);
    String rejectionField = jobExecutionContext.getMergedJobDataMap().getString(REJECTION_FIELD);
    String rejectionValue = jobExecutionContext.getMergedJobDataMap().getString(REJECTION_VALUE);
    String workflowExecutionId = jobExecutionContext.getMergedJobDataMap().getString(WORKFLOW_EXECUTION_ID);
    String ticketType = jobExecutionContext.getMergedJobDataMap().getString(TICKET_TYPE);

    logger.info("Polling Approval Status for approvalId {}", approvalId);
    try {
      ApprovalDetails.Action approval = serviceNowService.getApprovalStatus(connectorId, accountId, appId, issueNumber,
          approvalField, approvalValue, rejectionField, rejectionValue, ticketType);
      if (approval != null) {
        logger.info("Servicenow Approval Status: {} for approvalId: {}, workflowExecutionId: {} ", approval, approvalId,
            workflowExecutionId);
        logger.info("Deleting job for approvalId: {}, workflowExecutionId: {} ", approvalId, workflowExecutionId);
        jobScheduler.deleteJob(approvalId, GROUP);
        EmbeddedUser user = null;
        serviceNowService.approveWorkflow(approval, approvalId, user, appId, workflowExecutionId);
      }

    } catch (WingsException we) {
      logger.info("Deleting job for approvalId: {}, workflowExecutionId: {} ", approvalId, workflowExecutionId);
      jobScheduler.deleteJob(approvalId, GROUP);
      throw we;
    } catch (Exception ex) {
      logger.error("Exception in execute JiraPollingJob. approvalId: {}, workflowExecutionId: {} ", approvalId,
          workflowExecutionId, ex);
      throw new WingsException(ErrorCode.SERVICENOW_ERROR, WingsException.USER, ex)
          .addParam("message", "Exception in fetching approval " + ExceptionUtils.getMessage(ex));
    }
  }

  public static void deleteJob(PersistentScheduler jobScheduler, String approvalId) {
    jobScheduler.deleteJob(approvalId, GROUP);
  }
}
