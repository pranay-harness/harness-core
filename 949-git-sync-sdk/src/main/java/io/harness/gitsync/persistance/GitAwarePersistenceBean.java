package io.harness.gitsync.persistance;

import com.google.inject.Injector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.guice.annotation.EnableGuiceModules;

@EnableGuiceModules
@Configuration
public class GitAwarePersistenceBean {
  @Autowired private Injector injector;

  @Bean
  public GitAwarePersistence myModule() {
    return injector.getInstance(GitAwarePersistence.class);
  }
}
