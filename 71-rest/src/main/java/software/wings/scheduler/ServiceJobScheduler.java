package software.wings.scheduler;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import software.wings.app.MainConfiguration;

@Singleton
@Slf4j
public class ServiceJobScheduler extends JobScheduler {
  @Inject
  public ServiceJobScheduler(Injector injector, MainConfiguration configuration) {
    super(injector, configuration.getServiceSchedulerConfig(), configuration.getMongoConnectionFactory().getUri());
  }
}
