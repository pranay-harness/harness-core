package software.wings.utils;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static software.wings.common.Constants.ONE_TIME_REMINDER;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import software.wings.common.Constants;
import software.wings.scheduler.QuartzScheduler;
import software.wings.scheduler.ReminderNotifyJob;

import java.util.Date;
import java.util.Map;

public class CronUtil {
  @Inject @Named("JobScheduler") private QuartzScheduler jobScheduler;

  public String scheduleReminder(long waitMillis) {
    return scheduleReminder(waitMillis, null);
  }

  public String scheduleReminder(long waitMillis, String paramName, String paramValue) {
    return scheduleReminder(waitMillis, ImmutableMap.of(paramName, paramValue));
  }

  public String scheduleReminder(long waitMillis, Map<String, String> parameters) {
    String resumeId = generateUuid();
    long wakeupTs = System.currentTimeMillis() + waitMillis;
    JobBuilder jobBuilder = JobBuilder.newJob(ReminderNotifyJob.class).withIdentity(resumeId, ONE_TIME_REMINDER);
    if (parameters != null) {
      parameters.keySet().forEach(key -> { jobBuilder.usingJobData(key, parameters.get(key)); });
    }
    jobBuilder.usingJobData(Constants.CORRELATION_ID, resumeId);

    JobDetail job = jobBuilder.build();
    Trigger trigger = TriggerBuilder.newTrigger()
                          .withIdentity(resumeId, ONE_TIME_REMINDER)
                          .startAt(new Date(wakeupTs))
                          .forJob(job)
                          .build();
    jobScheduler.scheduleJob(job, trigger);

    return resumeId;
  }
}
