package software.wings.scheduler;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.app.MainConfiguration;

@Singleton
public class BackgroundJobScheduler extends JobScheduler {
  private static final Logger logger = LoggerFactory.getLogger(BackgroundJobScheduler.class);

  @Inject
  public BackgroundJobScheduler(Injector injector, MainConfiguration configuration) {
    super(injector, configuration.getBackgroundSchedulerConfig(), configuration.getMongoConnectionFactory().getUri());
  }
}
