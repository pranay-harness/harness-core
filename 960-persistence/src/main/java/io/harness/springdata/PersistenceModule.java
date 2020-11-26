package io.harness.springdata;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import com.google.inject.AbstractModule;
import org.springframework.guice.module.BeanFactoryProvider;
import org.springframework.guice.module.SpringModule;

@OwnedBy(PL)
public abstract class PersistenceModule extends AbstractModule {
  @Override
  protected void configure() {
    install(new SpringModule(BeanFactoryProvider.from(getConfigClasses())));
  }

  protected abstract Class<?>[] getConfigClasses();
}
