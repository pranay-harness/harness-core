package software.wings.scheduler;

import org.quartz.JobDetail;
import org.quartz.Trigger;

import java.util.Date;

public interface QuartzScheduler {
  Date scheduleJob(JobDetail jobDetail, Trigger trigger);
  boolean deleteJob(String jobName, String groupName);
}
