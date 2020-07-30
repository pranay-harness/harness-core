package io.harness.ng;

import com.google.inject.AbstractModule;
import com.google.inject.Module;

import io.harness.mongo.MongoModule;
import org.springframework.guice.module.BeanFactoryProvider;
import org.springframework.guice.module.SpringModule;

public abstract class PersistenceModule extends AbstractModule {
  @Override
  protected void configure() {
    install(new SpringModule(BeanFactoryProvider.from(getConfigClasses())));
    install(getMongoModule());
  }

  protected Module getMongoModule() {
    return MongoModule.getInstance();
  }

  protected abstract Class<? extends SpringPersistenceConfig>[] getConfigClasses();
}