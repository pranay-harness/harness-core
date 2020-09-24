package io.harness.mongo;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.harness.morphia.MorphiaModule;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.ObjectFactory;

import java.util.Map;

@Slf4j
public class ObjectFactoryModule extends AbstractModule {
  private static volatile ObjectFactoryModule instance;

  public static ObjectFactoryModule getInstance() {
    if (instance == null) {
      instance = new ObjectFactoryModule();
    }
    return instance;
  }

  private ObjectFactoryModule() {}

  @Provides
  @Singleton
  public ObjectFactory objectFactory(
      @Named("morphiaInterfaceImplementersClasses") Map<String, Class> morphiaInterfaceImplementersClasses) {
    return new HObjectFactory(morphiaInterfaceImplementersClasses);
  }

  @Override
  protected void configure() {
    install(MorphiaModule.getInstance());
  }
}
