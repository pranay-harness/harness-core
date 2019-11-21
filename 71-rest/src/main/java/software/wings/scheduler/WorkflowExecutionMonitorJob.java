package software.wings.scheduler;

import static io.harness.beans.ExecutionStatus.ERROR;
import static io.harness.beans.ExecutionStatus.EXPIRED;
import static io.harness.beans.ExecutionStatus.NEW;
import static io.harness.beans.ExecutionStatus.PAUSED;
import static io.harness.beans.ExecutionStatus.RUNNING;
import static io.harness.beans.ExecutionStatus.STARTING;
import static io.harness.beans.ExecutionStatus.WAITING;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static io.harness.persistence.HQuery.excludeAuthority;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static software.wings.sm.ExecutionInterrupt.ExecutionInterruptBuilder.anExecutionInterrupt;
import static software.wings.sm.ExecutionInterruptType.MARK_EXPIRED;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.exception.WingsException;
import io.harness.logging.AutoLogContext;
import io.harness.logging.ExceptionLogger;
import io.harness.persistence.HIterator;
import io.harness.scheduler.PersistentScheduler;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Sort;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionKeys;
import software.wings.dl.WingsPersistence;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionInterrupt;
import software.wings.sm.ExecutionInterruptManager;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateExecutionInstance.StateExecutionInstanceKeys;
import software.wings.sm.StateMachineExecutor;

import java.time.Duration;
import java.util.concurrent.ExecutorService;

@Slf4j
public class WorkflowExecutionMonitorJob implements Job {
  public static final String NAME = "OBSERVER";
  public static final String GROUP = "WORKFLOW_MONITOR_CRON_GROUP";
  private static final Duration POLL_INTERVAL = Duration.ofMinutes(1);
  private static final Duration INACTIVITY_TIMEOUT = Duration.ofSeconds(30);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private ExecutionInterruptManager executionInterruptManager;
  @Inject private ExecutorService executorService;
  @Inject private StateMachineExecutor stateMachineExecutor;

  @Inject @Named("BackgroundJobScheduler") private PersistentScheduler jobScheduler;

  public static void add(PersistentScheduler jobScheduler) {
    JobDetail job = JobBuilder.newJob(WorkflowExecutionMonitorJob.class).withIdentity(NAME, GROUP).build();

    Trigger trigger = TriggerBuilder.newTrigger()
                          .withIdentity(NAME, GROUP)
                          .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                                            .withIntervalInMinutes((int) POLL_INTERVAL.toMinutes())
                                            .repeatForever())
                          .build();

    jobScheduler.ensureJob__UnderConstruction(job, trigger);
  }

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    executorService.submit(() -> checkForExpiryWorkflow());
  }

  public void checkForExpiryWorkflow() {
    try (HIterator<WorkflowExecution> workflowExecutions =
             new HIterator<>(wingsPersistence.createQuery(WorkflowExecution.class, excludeAuthority)
                                 .field(WorkflowExecutionKeys.status)
                                 .in(asList(RUNNING, NEW, STARTING, PAUSED, WAITING))
                                 .fetch())) {
      while (workflowExecutions.hasNext()) {
        WorkflowExecution workflowExecution = workflowExecutions.next();

        boolean hasActiveStates = false;
        try (HIterator<StateExecutionInstance> stateExecutionInstances =
                 new HIterator<>(wingsPersistence.createQuery(StateExecutionInstance.class)
                                     .filter(StateExecutionInstanceKeys.appId, workflowExecution.getAppId())
                                     .filter(StateExecutionInstanceKeys.executionUuid, workflowExecution.getUuid())
                                     .field(StateExecutionInstanceKeys.status)
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
                                                        .executionInterruptType(MARK_EXPIRED)
                                                        .appId(stateExecutionInstance.getAppId())
                                                        .executionUuid(stateExecutionInstance.getExecutionUuid())
                                                        .stateExecutionInstanceId(stateExecutionInstance.getUuid())
                                                        .build();

            executionInterruptManager.registerExecutionInterrupt(executionInterrupt);
          }
        } catch (WingsException exception) {
          ExceptionLogger.logProcessedMessages(exception, MANAGER, logger);
        } catch (Exception e) {
          logger.error(format("Error in cleaning up the workflow execution %s", workflowExecution.getUuid()), e);
        }

        if (!hasActiveStates) {
          logger.warn("WorkflowExecution {} is in non final state, but there is no active state execution for it.",
              workflowExecution.getUuid());

          final StateExecutionInstance stateExecutionInstance =
              wingsPersistence.createQuery(StateExecutionInstance.class)
                  .filter(StateExecutionInstanceKeys.appId, workflowExecution.getAppId())
                  .filter(StateExecutionInstanceKeys.executionUuid, workflowExecution.getUuid())
                  .field(StateExecutionInstanceKeys.notifyId)
                  .doesNotExist()
                  .field(StateExecutionInstanceKeys.callback)
                  .exists()
                  .order(Sort.descending(StateExecutionInstanceKeys.lastUpdatedAt))
                  .get();

          if (stateExecutionInstance == null) {
            logger.error("Workflow execution stuck, but we cannot find good state to callback from. This is so wrong!");
            continue;
          }

          if (stateExecutionInstance.getLastUpdatedAt() > System.currentTimeMillis() - INACTIVITY_TIMEOUT.toMillis()) {
            logger.warn("WorkflowExecution {} last callbackable state {} is very recent."
                    + "Lets give more time to the system it might be just in the middle of things.",
                workflowExecution.getUuid(), stateExecutionInstance.getUuid());
            continue;
          }

          final ExecutionContextImpl executionContext =
              stateMachineExecutor.getExecutionContext(stateExecutionInstance.getAppId(),
                  stateExecutionInstance.getExecutionUuid(), stateExecutionInstance.getUuid());

          try (AutoLogContext ignore = executionContext.autoLogContext()) {
            boolean expired =
                workflowExecution.getCreatedAt() < System.currentTimeMillis() - WorkflowExecution.EXPIRY.toMillis();
            // We lost the eventual exception, but its better than doing nothing
            stateMachineExecutor.executeCallback(
                executionContext, stateExecutionInstance, expired ? EXPIRED : ERROR, null);
          }
        }
      }
    } catch (WingsException exception) {
      ExceptionLogger.logProcessedMessages(exception, MANAGER, logger);
    } catch (Exception e) {
      logger.error("Error in monitoring the workflow executions ", e);
    }
  }
}
