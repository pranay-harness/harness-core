package migrations.all;

import com.google.inject.Inject;
import com.google.inject.Injector;

import migrations.Migration;
import software.wings.app.MainConfiguration;
import software.wings.scheduler.JobScheduler;

/**
 * Created by rsingh on 4/30/18.
 */
public class NewRelicMetricNameCronRemoval implements Migration {
  @Inject Injector injector;
  @Inject MainConfiguration configuration;

  @Override
  public void migrate() {
    JobScheduler jobScheduler = new JobScheduler(injector, configuration);
    jobScheduler.deleteJob("NEW_RELIC_METRIC_NAME_COLLECT_CRON", "NEW_RELIC_METRIC_NAME_COLLECT_CRON_GROUP");
  }
}
