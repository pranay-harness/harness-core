package io.harness.pms.sdk;

import com.google.inject.AbstractModule;
import org.springframework.guice.module.BeanFactoryProvider;
import org.springframework.guice.module.SpringModule;

public class PmsSdkPersistenceModule extends AbstractModule {
  private static PmsSdkPersistenceModule instance;

  public static PmsSdkPersistenceModule getInstance() {
    if (instance == null) {
      instance = new PmsSdkPersistenceModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    install(new SpringModule(BeanFactoryProvider.from(PmsSdkPersistenceConfig.class)));
  }
}