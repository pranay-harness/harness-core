package io.harness.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.quartz.JobDetail;
import org.quartz.Trigger;

public class HQuartzSchedulerTest {
  @Test
  public void testJobCompare() {
    JobDetail nullJob = null;
    assertThat(HQuartzScheduler.compare(nullJob, nullJob)).isTrue();
  }

  @Test
  public void testTriggerCompare() {
    Trigger nullTrigger = null;
    assertThat(HQuartzScheduler.compare(nullTrigger, nullTrigger)).isTrue();
  }
}
