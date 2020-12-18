package io.harness.pms.sdk;

import io.harness.serializer.PmsSdkModuleRegistrars;

import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import java.util.List;
import org.springframework.core.convert.converter.Converter;
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

  @Provides
  @Singleton
  List<Class<? extends Converter<?, ?>>> springConverters() {
    return ImmutableList.<Class<? extends Converter<?, ?>>>builder()
        .addAll(PmsSdkModuleRegistrars.springConverters)
        .build();
  }
}
