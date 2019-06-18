package software.wings.scheduler;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.data.structure.EmptyPredicate;
import io.harness.scheduler.PersistentScheduler;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import software.wings.beans.approval.ShellScriptApprovalParams;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @deprecated Migrated to mongo persistent iterator. Only left for handling old jobs.
 * TODO: Delete this class.
 */
@Deprecated
@Slf4j
public class ScriptApprovalJob implements Job {
  private static final String SCRIPT_STRING_KEY = "SCRIPT_STRING_KEY";
  private static final String ACTIVITY_ID_KEY = "activityId";
  private static final String APPROVAL_ID_KEY = "approvalId";

  private static final int DELAY_START_SECONDS = 10;

  @Inject @Named("BackgroundJobScheduler") private PersistentScheduler jobScheduler;
  @Inject ShellScriptApprovalService shellScriptApprovalService;

  private static String SCRIPT_APPROVAL_JOB_GROUP = "SHELL_SCRIPT_APPROVAL_JOB";
  private static String ACCOUNT_ID_KEY = "accountId";
  private static String APP_ID_KEY = "appId";
  private static String WORKFLOW_EXECUTION_ID_KEY = "workflowExecutionId";

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

    if (shellScriptApprovalService.tryShellScriptApproval(accountId, appId, approvalId, activityId, scriptString)) {
      jobScheduler.deleteJob(approvalId, SCRIPT_APPROVAL_JOB_GROUP);
    }
  }
}
