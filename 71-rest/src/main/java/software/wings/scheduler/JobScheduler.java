package software.wings.scheduler;

import static io.harness.maintenance.MaintenanceController.isMaintenance;
import static java.util.Arrays.asList;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.maintenance.MaintenanceController;
import io.harness.scheduler.HQuartzScheduler;
import io.harness.scheduler.SchedulerConfig;
import lombok.extern.slf4j.Slf4j;
import org.quartz.SchedulerException;
import software.wings.core.managerConfiguration.ConfigChangeEvent;
import software.wings.core.managerConfiguration.ConfigChangeListener;
import software.wings.core.managerConfiguration.ConfigurationController;

import java.util.List;

@Slf4j
public class JobScheduler extends HQuartzScheduler implements ConfigChangeListener {
  @Inject
  public JobScheduler(Injector injector, SchedulerConfig schedulerConfig, String defaultMongoUri) {
    super(injector, schedulerConfig, defaultMongoUri);
    try {
      if (schedulerConfig.getAutoStart().equals("true")) {
        injector.getInstance(MaintenanceController.class).register(this);
        scheduler = createScheduler(getDefaultProperties());
        injector.getInstance(ConfigurationController.class).register(this, asList(ConfigChangeEvent.PrimaryChanged));

        ConfigurationController configurationController = injector.getInstance(Key.get(ConfigurationController.class));
        if (!isMaintenance() && configurationController.isPrimary()) {
          scheduler.start();
        }
      }
    } catch (SchedulerException exception) {
      throw new WingsException(ErrorCode.UNKNOWN_ERROR, exception)
          .addParam("message", "Could not initialize cron scheduler");
    }
  }

  @Override
  public void onConfigChange(List<ConfigChangeEvent> events) {
    logger.info("onConfigChange {}", events);

    if (scheduler != null) {
      if (events.contains(ConfigChangeEvent.PrimaryChanged)) {
        ConfigurationController configurationController = injector.getInstance(Key.get(ConfigurationController.class));
        try {
          if (configurationController.isPrimary()) {
            scheduler.start();
          } else {
            scheduler.standby();
          }
        } catch (SchedulerException e) {
          logger.error("Error updating scheduler.", e);
        }
      }
    }
  }
}
