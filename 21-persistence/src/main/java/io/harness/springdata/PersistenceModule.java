package io.harness.springdata;

import static io.harness.annotations.dev.HarnessTeam.PL;

import com.google.inject.AbstractModule;

import io.harness.annotations.dev.OwnedBy;
import org.springframework.guice.module.BeanFactoryProvider;
import org.springframework.guice.module.SpringModule;

@OwnedBy(PL)
public abstract class PersistenceModule extends AbstractModule {
  @Override
  protected void configure() {
    install(new SpringModule(BeanFactoryProvider.from(getConfigClasses())));
  }

  protected abstract Class<? extends SpringPersistenceConfig>[] getConfigClasses();
}