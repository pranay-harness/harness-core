package software.wings.scheduler;

import static java.util.Arrays.asList;
import static software.wings.dl.HQuery.excludeAuthority;
import static software.wings.exception.WingsException.ExecutionContext.MANAGER;
import static software.wings.sm.ExecutionInterrupt.ExecutionInterruptBuilder.anExecutionInterrupt;
import static software.wings.sm.ExecutionInterruptType.ABORT;
import static software.wings.sm.ExecutionStatus.NEW;
import static software.wings.sm.ExecutionStatus.PAUSED;
import static software.wings.sm.ExecutionStatus.RUNNING;
import static software.wings.sm.ExecutionStatus.STARTING;
import static software.wings.sm.ExecutionStatus.WAITING;

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
import software.wings.beans.WorkflowExecution;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.sm.ExecutionInterrupt;
import software.wings.sm.ExecutionInterruptManager;
import software.wings.sm.StateExecutionInstance;

import java.time.Duration;
import java.util.concurrent.ExecutorService;

public class WorkflowExecutionMonitorJob implements Job {
  private static final Logger logger = LoggerFactory.getLogger(WorkflowExecutionMonitorJob.class);

  public static final String NAME = "OBSERVER";
  public static final String GROUP = "WORKFLOW_MONITOR_CRON_GROUP";
  private static final Duration POLL_INTERVAL = Duration.ofMinutes(1);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private ExecutionInterruptManager executionInterruptManager;
  @Inject private ExecutorService executorService;

  @Inject @Named("JobScheduler") private QuartzScheduler jobScheduler;

  public static void add(QuartzScheduler jobScheduler) {
    jobScheduler.deleteJob(NAME, GROUP);
    JobDetail job = JobBuilder.newJob(WorkflowExecutionMonitorJob.class).withIdentity(NAME, GROUP).build();

    Trigger trigger = TriggerBuilder.newTrigger()
                          .withIdentity(NAME, GROUP)
                          .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                                            .withIntervalInMinutes((int) POLL_INTERVAL.toMinutes())
                                            .repeatForever())
                          .build();

    jobScheduler.scheduleJob(job, trigger);
  }

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    executorService.submit(() -> asyncExecute());
  }

  public void asyncExecute() {
    final MorphiaIterator<WorkflowExecution, WorkflowExecution> workflowExecutions =
        wingsPersistence.createQuery(WorkflowExecution.class, excludeAuthority)
            .field(WorkflowExecution.STATUS_KEY)
            .in(asList(RUNNING, NEW, STARTING, PAUSED, WAITING))
            .fetch();

    try (DBCursor cursor = workflowExecutions.getCursor()) {
      while (workflowExecutions.hasNext()) {
        WorkflowExecution workflowExecution = workflowExecutions.next();

        final MorphiaIterator<StateExecutionInstance, StateExecutionInstance> stateExecutionInstances =
            wingsPersistence.createQuery(StateExecutionInstance.class)
                .filter(WorkflowExecution.APP_ID_KEY, workflowExecution.getAppId())
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
          exception.logProcessedMessages(MANAGER, logger);
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
      exception.logProcessedMessages(MANAGER, logger);
    } catch (Exception e) {
      logger.error("Error in monitoring the workflow executions ", e);
    }
  }
}
