package io.harness.springdata;

import com.google.inject.AbstractModule;

import org.springframework.guice.module.BeanFactoryProvider;
import org.springframework.guice.module.SpringModule;

public abstract class PersistenceModule extends AbstractModule {
  @Override
  protected void configure() {
    install(new SpringModule(BeanFactoryProvider.from(getConfigClasses())));
  }

  protected abstract Class<? extends SpringPersistenceConfig>[] getConfigClasses();
}