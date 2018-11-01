package software.wings.scheduler;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.app.MainConfiguration;

@Singleton
public class ServiceJobScheduler extends JobScheduler {
  private static final Logger logger = LoggerFactory.getLogger(ServiceJobScheduler.class);

  @Inject
  public ServiceJobScheduler(Injector injector, MainConfiguration configuration) {
    super(injector, configuration.getServiceSchedulerConfig(), configuration.getMongoConnectionFactory().getUri());
  }
}
