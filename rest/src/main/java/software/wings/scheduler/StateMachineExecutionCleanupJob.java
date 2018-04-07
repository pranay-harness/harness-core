package software.wings.scheduler;

import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.sm.ExecutionInterrupt.ExecutionInterruptBuilder.anExecutionInterrupt;
import static software.wings.sm.ExecutionInterruptType.ABORT;
import static software.wings.sm.ExecutionStatus.NEW;
import static software.wings.sm.ExecutionStatus.PAUSED;
import static software.wings.sm.ExecutionStatus.RUNNING;
import static software.wings.sm.ExecutionStatus.STARTING;
import static software.wings.sm.ExecutionStatus.WAITING;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.WorkflowExecution;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.sm.ExecutionInterrupt;
import software.wings.sm.ExecutionInterruptManager;
import software.wings.sm.StateExecutionInstance;

/**
 * Created by rishi on 4/6/17.
 */
public class StateMachineExecutionCleanupJob implements Job {
  private static final Logger logger = LoggerFactory.getLogger(StateMachineExecutionCleanupJob.class);

  public static final String GROUP = "SM_CLEANUP_CRON_GROUP";
  private static final int POLL_INTERVAL = 60;

  public static final String APP_ID_KEY = "appId";

  @Inject private WingsPersistence wingsPersistence;
  @Inject private ExecutionInterruptManager executionInterruptManager;

  @Inject @Named("JobScheduler") private QuartzScheduler jobScheduler;

  public static void add(QuartzScheduler jobScheduler, String appId) {
    jobScheduler.deleteJob(appId, GROUP);

    JobDetail job = JobBuilder.newJob(StateMachineExecutionCleanupJob.class)
                        .withIdentity(appId, GROUP)
                        .usingJobData(StateMachineExecutionCleanupJob.APP_ID_KEY, appId)
                        .build();

    Trigger trigger =
        TriggerBuilder.newTrigger()
            .withIdentity(appId, GROUP)
            .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(POLL_INTERVAL).repeatForever())
            .build();

    jobScheduler.scheduleJob(job, trigger);
  }

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    String appId = jobExecutionContext.getMergedJobDataMap().getString(APP_ID_KEY);

    // This is making the job self pruning. This allow to simplify the logic in deletion of the application.
    Application application = wingsPersistence.get(Application.class, appId);
    if (application == null) {
      jobScheduler.deleteJob(appId, GROUP);
      return;
    }
    executeInternal(appId);
  }

  private void executeInternal(String appId) {
    PageRequest<WorkflowExecution> pageRequest =
        aPageRequest()
            .withLimit(PageRequest.UNLIMITED)
            .addFilter(APP_ID_KEY, Operator.EQ, appId)
            .addFilter("status", Operator.IN, RUNNING, NEW, STARTING, PAUSED, WAITING)
            .build();
    PageResponse<WorkflowExecution> executionPageResponse =
        wingsPersistence.query(WorkflowExecution.class, pageRequest);
    for (WorkflowExecution workflowExecution : executionPageResponse.getResponse()) {
      try {
        PageResponse<StateExecutionInstance> pageResponse = wingsPersistence.query(StateExecutionInstance.class,
            aPageRequest()
                .addFilter("status", Operator.IN, RUNNING, NEW, STARTING, PAUSED, WAITING)
                .addFilter("expiryTs", Operator.LT, System.currentTimeMillis())
                .addFilter("executionUuid", Operator.EQ, workflowExecution.getUuid())
                .withLimit("1000")
                .addFilter(APP_ID_KEY, Operator.EQ, appId)
                .build());
        for (StateExecutionInstance stateExecutionInstance : pageResponse) {
          try {
            logger.info("Expired StateExecutionInstance found: {}", stateExecutionInstance.getUuid());
            ExecutionInterrupt executionInterrupt = anExecutionInterrupt()
                                                        .withExecutionInterruptType(ABORT)
                                                        .withAppId(stateExecutionInstance.getAppId())
                                                        .withExecutionUuid(stateExecutionInstance.getExecutionUuid())
                                                        .withStateExecutionInstanceId(stateExecutionInstance.getUuid())
                                                        .build();

            executionInterruptManager.registerExecutionInterrupt(executionInterrupt);
          } catch (WingsException e) {
            logger.error("Error in interrupt for stateExecutionInstance: {}", stateExecutionInstance.getUuid(), e);
          }
        }
      } catch (Exception e) {
        logger.error("Error in cleaning up the workflowExecution {}", workflowExecution.getUuid(), e);
      }
    }
  }
}
