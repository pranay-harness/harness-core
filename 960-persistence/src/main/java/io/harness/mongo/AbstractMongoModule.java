package io.harness.mongo;

import io.harness.persistence.UserProvider;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractMongoModule extends AbstractModule {
  @Override
  protected void configure() {
    install(MongoModule.getInstance());
  }

  @Provides
  @Singleton
  protected UserProvider injectUserProvider() {
    return userProvider();
  };

  public abstract UserProvider userProvider();
}
