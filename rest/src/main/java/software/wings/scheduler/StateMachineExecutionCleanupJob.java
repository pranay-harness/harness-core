package software.wings.scheduler;

import static java.util.Arrays.asList;
import static software.wings.sm.ExecutionInterrupt.ExecutionInterruptBuilder.anExecutionInterrupt;
import static software.wings.sm.ExecutionInterruptType.ABORT;
import static software.wings.sm.ExecutionStatus.NEW;
import static software.wings.sm.ExecutionStatus.PAUSED;
import static software.wings.sm.ExecutionStatus.RUNNING;
import static software.wings.sm.ExecutionStatus.STARTING;
import static software.wings.sm.ExecutionStatus.WAITING;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import com.mongodb.DBCursor;
import org.mongodb.morphia.query.MorphiaIterator;
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
import software.wings.beans.WorkflowExecution;
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

  @VisibleForTesting
  void executeInternal(String appId) {
    final MorphiaIterator<WorkflowExecution, WorkflowExecution> workflowExecutions =
        wingsPersistence.createQuery(WorkflowExecution.class)
            .filter(WorkflowExecution.APP_ID_KEY, appId)
            .field(WorkflowExecution.STATUS_KEY)
            .in(asList(RUNNING, NEW, STARTING, PAUSED, WAITING))
            .fetch();

    try (DBCursor cursor = workflowExecutions.getCursor()) {
      while (workflowExecutions.hasNext()) {
        WorkflowExecution workflowExecution = workflowExecutions.next();

        final MorphiaIterator<StateExecutionInstance, StateExecutionInstance> stateExecutionInstances =
            wingsPersistence.createQuery(StateExecutionInstance.class)
                .filter(WorkflowExecution.APP_ID_KEY, appId)
                .filter(StateExecutionInstance.EXECUTION_UUID_KEY, workflowExecution.getUuid())
                .field(StateExecutionInstance.STATUS_KEY)
                .in(asList(RUNNING, NEW, STARTING, PAUSED, WAITING))
                .fetch();

        boolean hasActiveStates = stateExecutionInstances.hasNext();
        try (DBCursor ignored = stateExecutionInstances.getCursor()) {
          while (stateExecutionInstances.hasNext()) {
            StateExecutionInstance stateExecutionInstance = stateExecutionInstances.next();
            if (stateExecutionInstance.getExpiryTs() > System.currentTimeMillis()) {
              continue;
            }

            logger.info("Expired StateExecutionInstance found: {}", stateExecutionInstance.getUuid());
            ExecutionInterrupt executionInterrupt = anExecutionInterrupt()
                                                        .withExecutionInterruptType(ABORT)
                                                        .withAppId(stateExecutionInstance.getAppId())
                                                        .withExecutionUuid(stateExecutionInstance.getExecutionUuid())
                                                        .withStateExecutionInstanceId(stateExecutionInstance.getUuid())
                                                        .build();

            executionInterruptManager.registerExecutionInterrupt(executionInterrupt);
          }
        } catch (WingsException exception) {
          exception.logProcessedMessages(logger);
        } catch (Exception e) {
          logger.error("Error in cleaning up the workflow execution {}", workflowExecution.getUuid(), e);
        }

        if (!hasActiveStates
            && workflowExecution.getCreatedAt() < System.currentTimeMillis() + WorkflowExecution.EXPIRY.toMillis()) {
          logger.error("WorkflowExecution {} is in non final state, but there is no active state execution for it.",
              workflowExecution.getUuid());
          // TODO: enable this force fix of workflow execution if needed
          //          Query<WorkflowExecution> query = wingsPersistence.createQuery(WorkflowExecution.class)
          //                                               .filter(WorkflowExecution.APP_ID_KEY,
          //                                               workflowExecution.getAppId())
          //                                               .filter(WorkflowExecution.ID_KEY,
          //                                               workflowExecution.getUuid());
          //          UpdateOperations<WorkflowExecution> updateOps =
          //              wingsPersistence.createUpdateOperations(WorkflowExecution.class)
          //                  .set("status", ExecutionStatus.ABORTED)
          //                  .set("endTs", System.currentTimeMillis());
          //          wingsPersistence.update(query, updateOps);
        }
      }
    } catch (WingsException exception) {
      exception.logProcessedMessages(logger);
    } catch (Exception e) {
      logger.error("Error in cleaning up the workflow executions for application {}", appId, e);
    }
  }
}
