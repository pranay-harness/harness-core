package software.wings.scheduler;

import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static io.harness.persistence.HQuery.excludeAuthority;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;
import static software.wings.sm.ExecutionInterrupt.ExecutionInterruptBuilder.anExecutionInterrupt;
import static software.wings.sm.ExecutionInterruptType.MARK_EXPIRED;
import static software.wings.sm.ExecutionStatus.NEW;
import static software.wings.sm.ExecutionStatus.PAUSED;
import static software.wings.sm.ExecutionStatus.RUNNING;
import static software.wings.sm.ExecutionStatus.STARTING;
import static software.wings.sm.ExecutionStatus.WAITING;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.exception.WingsException;
import io.harness.persistence.HIterator;
import org.mongodb.morphia.query.Sort;
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
import software.wings.exception.WingsExceptionMapper;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionInterrupt;
import software.wings.sm.ExecutionInterruptManager;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateMachineExecutor;

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
  @Inject private StateMachineExecutor stateMachineExecutor;

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
    executorService.submit(() -> checkForExpiryWorkflow());
  }

  public void checkForExpiryWorkflow() {
    try (HIterator<WorkflowExecution> workflowExecutions =
             new HIterator<>(wingsPersistence.createQuery(WorkflowExecution.class, excludeAuthority)
                                 .field(WorkflowExecution.STATUS_KEY)
                                 .in(asList(RUNNING, NEW, STARTING, PAUSED, WAITING))
                                 .fetch())) {
      while (workflowExecutions.hasNext()) {
        WorkflowExecution workflowExecution = workflowExecutions.next();

        boolean hasActiveStates = false;
        try (HIterator<StateExecutionInstance> stateExecutionInstances =
                 new HIterator<>(wingsPersistence.createQuery(StateExecutionInstance.class)
                                     .filter(WorkflowExecution.APP_ID_KEY, workflowExecution.getAppId())
                                     .filter(StateExecutionInstance.EXECUTION_UUID_KEY, workflowExecution.getUuid())
                                     .field(StateExecutionInstance.STATUS_KEY)
                                     .in(asList(RUNNING, NEW, STARTING, PAUSED, WAITING))
                                     .fetch())) {
          hasActiveStates = stateExecutionInstances.hasNext();
          while (stateExecutionInstances.hasNext()) {
            StateExecutionInstance stateExecutionInstance = stateExecutionInstances.next();
            if (stateExecutionInstance.getExpiryTs() > System.currentTimeMillis()) {
              continue;
            }

            logger.info("Expired StateExecutionInstance found: {}", stateExecutionInstance.getUuid());
            ExecutionInterrupt executionInterrupt = anExecutionInterrupt()
                                                        .withExecutionInterruptType(MARK_EXPIRED)
                                                        .withAppId(stateExecutionInstance.getAppId())
                                                        .withExecutionUuid(stateExecutionInstance.getExecutionUuid())
                                                        .withStateExecutionInstanceId(stateExecutionInstance.getUuid())
                                                        .build();

            executionInterruptManager.registerExecutionInterrupt(executionInterrupt);
          }
        } catch (WingsException exception) {
          WingsExceptionMapper.logProcessedMessages(exception, MANAGER, logger);
        } catch (Exception e) {
          logger.error(format("Error in cleaning up the workflow execution %s", workflowExecution.getUuid()), e);
        }

        if (!hasActiveStates
            && workflowExecution.getCreatedAt() < System.currentTimeMillis() + WorkflowExecution.EXPIRY.toMillis()) {
          logger.warn("WorkflowExecution {} is in non final state, but there is no active state execution for it.",
              workflowExecution.getUuid());

          final StateExecutionInstance stateExecutionInstance =
              wingsPersistence.createQuery(StateExecutionInstance.class)
                  .filter(StateExecutionInstance.APP_ID_KEY, workflowExecution.getAppId())
                  .filter(StateExecutionInstance.EXECUTION_UUID_KEY, workflowExecution.getUuid())
                  .field(StateExecutionInstance.NOTIFY_ID_KEY)
                  .doesNotExist()
                  .field(StateExecutionInstance.CALLBACK_KEY)
                  .exists()
                  .order(Sort.descending(StateExecutionInstance.LAST_UPDATED_AT_KEY))
                  .get();

          if (stateExecutionInstance == null) {
            logger.error("Workflow execution stuck, but we cannot find good state to callback from. This is so wrong!");
            continue;
          }

          if (stateExecutionInstance.getLastUpdatedAt() > System.currentTimeMillis() - ofSeconds(10).toMillis()) {
            logger.warn("WorkflowExecution {} last callbackable state {} is very recent."
                    + "Lets give more time to the system it might be just in the middle of things.",
                workflowExecution.getUuid(), stateExecutionInstance.getUuid());
            continue;
          }

          final ExecutionContextImpl executionContext =
              stateMachineExecutor.getExecutionContext(stateExecutionInstance.getAppId(),
                  stateExecutionInstance.getExecutionUuid(), stateExecutionInstance.getUuid());

          // We lost the eventual exception, but its better than doing nothing
          stateMachineExecutor.executeCallback(
              executionContext, stateExecutionInstance, stateExecutionInstance.getStatus(), null);
        }
      }
    } catch (WingsException exception) {
      WingsExceptionMapper.logProcessedMessages(exception, MANAGER, logger);
    } catch (Exception e) {
      logger.error("Error in monitoring the workflow executions ", e);
    }
  }
}
