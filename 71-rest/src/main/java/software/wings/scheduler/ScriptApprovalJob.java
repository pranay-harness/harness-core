package software.wings.scheduler;

import static software.wings.common.Constants.ACCOUNT_ID_KEY;
import static software.wings.common.Constants.APP_ID_KEY;
import static software.wings.common.Constants.SCRIPT_APPROVAL_COMMAND;
import static software.wings.common.Constants.SCRIPT_APPROVAL_ENV_VARIABLE;
import static software.wings.common.Constants.SCRIPT_APPROVAL_JOB_GROUP;
import static software.wings.common.Constants.WORKFLOW_EXECUTION_ID_KEY;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.beans.EmbeddedUser;
import io.harness.beans.ExecutionStatus;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.shell.ScriptType;
import io.harness.delegate.task.shell.ShellScriptApprovalTaskParameters;
import io.harness.scheduler.PersistentScheduler;
import io.harness.waiter.ErrorNotifyResponseData;
import io.harness.waiter.WaitNotifyEngine;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.ApprovalStateExecutionData;
import software.wings.api.ShellScriptApprovalExecutionData;
import software.wings.beans.ApprovalDetails.Action;
import software.wings.beans.DelegateTask;
import software.wings.beans.TaskType;
import software.wings.beans.approval.ShellScriptApprovalParams;
import software.wings.service.impl.DelegateServiceImpl;
import software.wings.service.intfc.WorkflowExecutionService;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class ScriptApprovalJob implements Job {
  private static final String SCRIPT_STRING_KEY = "SCRIPT_STRING_KEY";
  private static final String ACTIVITY_ID_KEY = "activityId";
  private static final String APPROVAL_ID_KEY = "approvalId";

  private static final long TIME_OUT_IN_MINUTES = 5;
  private static final int DELAY_START_SECONDS = 10;
  private static final String SCRIPT_APPROVAL_DIRECTORY = "/tmp";

  private static final Logger logger = LoggerFactory.getLogger(ScriptApprovalJob.class);

  @Inject @Named("BackgroundJobScheduler") private PersistentScheduler jobScheduler;

  @Inject private DelegateServiceImpl delegateService;
  @Inject WorkflowExecutionService workflowExecutionService;
  @Inject WaitNotifyEngine waitNotifyEngine;

  public static void doRetryJob(PersistentScheduler jobScheduler, String accountId, String appId, String approvalId,
      String workflowExecutionId, String activityId, ShellScriptApprovalParams shellScriptApprovalParams) {
    JobDetail job = JobBuilder.newJob(ScriptApprovalJob.class)
                        .withIdentity(approvalId, SCRIPT_APPROVAL_JOB_GROUP)
                        .usingJobData(ACCOUNT_ID_KEY, accountId)
                        .usingJobData(APP_ID_KEY, appId)
                        .usingJobData(APPROVAL_ID_KEY, approvalId)
                        .usingJobData(WORKFLOW_EXECUTION_ID_KEY, workflowExecutionId)
                        .usingJobData(ACTIVITY_ID_KEY, activityId)
                        .usingJobData(SCRIPT_STRING_KEY, shellScriptApprovalParams.getScriptString())
                        .build();

    Integer retryIntervalSeconds =
        Math.toIntExact(TimeUnit.MILLISECONDS.toSeconds(shellScriptApprovalParams.getRetryInterval()));

    Calendar cal = Calendar.getInstance();
    cal.add(Calendar.SECOND, DELAY_START_SECONDS);
    Date delay_start = cal.getTime();

    // TODO: @swagat handle timeout
    Trigger trigger = TriggerBuilder.newTrigger()
                          .withIdentity(approvalId, SCRIPT_APPROVAL_JOB_GROUP)
                          .startAt(delay_start)
                          .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                                            .withIntervalInSeconds(retryIntervalSeconds)
                                            .repeatForever()
                                            .withMisfireHandlingInstructionNowWithExistingCount())
                          .build();

    jobScheduler.ensureJob__UnderConstruction(job, trigger);
  }

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    String accountId = jobExecutionContext.getMergedJobDataMap().getString(ACCOUNT_ID_KEY);
    String appId = jobExecutionContext.getMergedJobDataMap().getString(APP_ID_KEY);
    String approvalId = jobExecutionContext.getMergedJobDataMap().getString(APPROVAL_ID_KEY);
    String workflowExecutionId = jobExecutionContext.getMergedJobDataMap().getString(WORKFLOW_EXECUTION_ID_KEY);
    String activityId = jobExecutionContext.getMergedJobDataMap().getString(ACTIVITY_ID_KEY);
    String scriptString = jobExecutionContext.getMergedJobDataMap().getString(SCRIPT_STRING_KEY);

    if (EmptyPredicate.isEmpty(accountId) || EmptyPredicate.isEmpty(appId)
        || EmptyPredicate.isEmpty(workflowExecutionId)) {
      if (EmptyPredicate.isNotEmpty(approvalId)) {
        jobScheduler.deleteJob(approvalId, SCRIPT_APPROVAL_JOB_GROUP);
      }
      return;
    }

    ShellScriptApprovalTaskParameters shellScriptApprovalTaskParameters =
        ShellScriptApprovalTaskParameters.builder()
            .accountId(accountId)
            .appId(appId)
            .activityId(activityId)
            .commandName(SCRIPT_APPROVAL_COMMAND)
            .outputVars(SCRIPT_APPROVAL_ENV_VARIABLE)
            .workingDirectory(SCRIPT_APPROVAL_DIRECTORY)
            .scriptType(ScriptType.BASH)
            .script(scriptString)
            .build();

    DelegateTask delegateTask = DelegateTask.builder()
                                    .accountId(accountId)
                                    .appId(appId)
                                    .waitId(activityId)
                                    .data(TaskData.builder()
                                              .taskType(TaskType.SHELL_SCRIPT_APPROVAL.name())
                                              .parameters(new Object[] {shellScriptApprovalTaskParameters})
                                              .timeout(TimeUnit.MINUTES.toMillis(TIME_OUT_IN_MINUTES))
                                              .build())
                                    .async(false)
                                    .build();

    boolean isTerminal = false;

    // TODO : @swagat make this async
    ResponseData responseData = null;
    try {
      responseData = delegateService.executeTask(delegateTask);
    } catch (Exception e) {
      logger.error("Failed to fetch Approval Status from Script", e);
      isTerminal = true;
    }

    if (responseData instanceof ShellScriptApprovalExecutionData) {
      ShellScriptApprovalExecutionData executionData = (ShellScriptApprovalExecutionData) responseData;
      if (executionData.getApprovalAction() == Action.APPROVE || executionData.getApprovalAction() == Action.REJECT) {
        isTerminal = true;

        try {
          approveWorkflow(approvalId, null, appId, workflowExecutionId, executionData.getExecutionStatus());
        } catch (Exception e) {
          logger.error("Failed to Approve/Reject Status", e);
        }
      }
    } else if (responseData instanceof ErrorNotifyResponseData) {
      logger.error("Shell Script Approval task failed unexpectedly.", (ErrorNotifyResponseData) responseData);
      isTerminal = true;
    }

    if (isTerminal) {
      jobScheduler.deleteJob(approvalId, SCRIPT_APPROVAL_JOB_GROUP);
    }
  }

  public void approveWorkflow(
      String approvalId, EmbeddedUser user, String appId, String workflowExecutionId, ExecutionStatus approvalStatus) {
    ApprovalStateExecutionData executionData = ApprovalStateExecutionData.builder()
                                                   .appId(appId)
                                                   .approvalId(approvalId)
                                                   .approvedOn(System.currentTimeMillis())
                                                   .build();

    if (approvalStatus == ExecutionStatus.SUCCESS || approvalStatus == ExecutionStatus.REJECTED) {
      executionData.setApprovedOn(System.currentTimeMillis());
    }

    executionData.setStatus(approvalStatus);
    if (user != null) {
      executionData.setApprovedBy(user);
    }
    waitNotifyEngine.notify(approvalId, executionData);
  }
}
