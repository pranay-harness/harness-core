package software.wings.scheduler;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;

import com.google.inject.Inject;

import io.fabric8.utils.Strings;
import io.harness.exception.WingsException;
import io.harness.scheduler.PersistentScheduler;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.exception.WingsExceptionMapper;
import software.wings.service.intfc.ResourceConstraintService;

import java.util.Set;

public class ResourceConstraintBackupJob implements Job {
  private static final Logger logger = LoggerFactory.getLogger(ResourceConstraintBackupJob.class);

  public static final String NAME = "BACKUP";
  public static final String GROUP = "RESOURCE_CONSTRAINT_GROUP";
  private static final int POLL_INTERVAL = 60;

  @Inject private ResourceConstraintService resourceConstraintService;

  public static Trigger trigger() {
    return TriggerBuilder.newTrigger()
        .withIdentity(NAME, GROUP)
        .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(POLL_INTERVAL).repeatForever())
        .build();
  }

  public static void addJob(PersistentScheduler jobScheduler) {
    // If somehow this job was scheduled from before, we would like to reset it to start counting from now.
    jobScheduler.deleteJob(NAME, GROUP);

    JobDetail details = JobBuilder.newJob(ResourceConstraintBackupJob.class).withIdentity(NAME, GROUP).build();
    jobScheduler.scheduleJob(details, trigger());
  }

  @Override
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    try {
      // Combine the constants that we just switched active to finished with all blocked ones.
      Set<String> completelyBlocked = resourceConstraintService.selectBlockedConstraints();
      if (isNotEmpty(completelyBlocked)) {
        if (logger.isWarnEnabled()) {
          logger.error("There are completely blocked constraints: {}", Strings.join(", ", completelyBlocked));
        }
      }

      Set<String> constraintIds = resourceConstraintService.updateActiveConstraints(null, null);
      constraintIds.addAll(completelyBlocked);

      // Unblock the constraints that can be unblocked
      resourceConstraintService.updateBlockedConstraints(constraintIds);
    } catch (WingsException exception) {
      WingsExceptionMapper.logProcessedMessages(exception, MANAGER, logger);
    } catch (RuntimeException e) {
      logger.error("", e);
    }
  }
}
