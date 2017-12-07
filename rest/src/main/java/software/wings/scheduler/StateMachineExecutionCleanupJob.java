package software.wings.scheduler;

import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.sm.ExecutionInterrupt.Builder.aWorkflowExecutionInterrupt;
import static software.wings.sm.ExecutionInterruptType.ABORT;
import static software.wings.sm.ExecutionStatus.NEW;
import static software.wings.sm.ExecutionStatus.PAUSED;
import static software.wings.sm.ExecutionStatus.RUNNING;
import static software.wings.sm.ExecutionStatus.STARTING;
import static software.wings.sm.ExecutionStatus.WAITING;

import com.google.inject.name.Named;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.beans.SearchFilter.Operator;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.sm.ExecutionInterrupt;
import software.wings.sm.ExecutionInterruptManager;
import software.wings.sm.StateExecutionInstance;

import javax.inject.Inject;

/**
 * Created by rishi on 4/6/17.
 */
public class StateMachineExecutionCleanupJob implements Job {
  private static final Logger logger = LoggerFactory.getLogger(StateMachineExecutionCleanupJob.class);

  public static final String GROUP = "SM_CLEANUP_CRON_GROUP";

  @Inject private WingsPersistence wingsPersistence;
  @Inject private ExecutionInterruptManager executionInterruptManager;

  @Inject @Named("JobScheduler") private QuartzScheduler jobScheduler;

  @Override
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    String appId = jobExecutionContext.getMergedJobDataMap().getString("appId");
    PageResponse<StateExecutionInstance> pageResponse = wingsPersistence.query(StateExecutionInstance.class,
        aPageRequest()
            .addFilter("status", Operator.IN, RUNNING, NEW, STARTING, PAUSED, WAITING)
            .addFilter("expiryTs", Operator.LT, System.currentTimeMillis())
            .withLimit("1000")
            .addFilter("appId", Operator.EQ, appId)
            .build());

    if (pageResponse == null || pageResponse.isEmpty()) {
      // This is making the job self pruning. This allow to simplify the logic in deletion of the application.
      // TODO: generalize this self pruning logic for every job.

      Application application = wingsPersistence.get(Application.class, appId);
      if (application == null) {
        jobScheduler.deleteJob(appId, GROUP);
      }
      return;
    }

    for (StateExecutionInstance stateExecutionInstance : pageResponse) {
      try {
        logger.info("Expired StateExecutionInstance found: {}", stateExecutionInstance.getUuid());
        ExecutionInterrupt executionInterrupt = aWorkflowExecutionInterrupt()
                                                    .withExecutionInterruptType(ABORT)
                                                    .withAppId(stateExecutionInstance.getAppId())
                                                    .withExecutionUuid(stateExecutionInstance.getExecutionUuid())
                                                    .withStateExecutionInstanceId(stateExecutionInstance.getUuid())
                                                    .build();

        executionInterruptManager.registerExecutionInterrupt(executionInterrupt);
      } catch (WingsException e) {
        logger.error("Error in interrupt for stateExecutionInstance: " + stateExecutionInstance.getUuid(), e);
      }
    }
  }
}
