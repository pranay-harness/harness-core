package io.harness.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.quartz.JobDetail;
import org.quartz.Trigger;

public class HQuartzSchedulerTest {
  @Test
  @Category(UnitTests.class)
  public void testJobCompare() {
    JobDetail nullJob = null;
    assertThat(HQuartzScheduler.compare(nullJob, nullJob)).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testTriggerCompare() {
    Trigger nullTrigger = null;
    assertThat(HQuartzScheduler.compare(nullTrigger, nullTrigger)).isTrue();
  }
}
