package io.harness.cvng.builderfactory;

import io.harness.cvng.cdng.entities.CVNGStepTask;
import io.harness.cvng.cdng.entities.CVNGStepTaskCreator;

import java.util.HashMap;
import java.util.Map;

public class BuilderFactoryModule {
  private final Map<Class<?>, Class<? extends DefaultObjectCreator>> defaultObjectCreatorMap;
  public BuilderFactoryModule() {
    defaultObjectCreatorMap = new HashMap<>();
    register();
  }
  private <T> void register(Class<T> builderClass, Class<? extends DefaultObjectCreator<T>> creatorClass) {
    defaultObjectCreatorMap.put(builderClass, creatorClass);
  }

  private void register() {
    register(CVNGStepTask.CVNGStepTaskBuilder.class, CVNGStepTaskCreator.class);
  }
  public <T> DefaultObjectCreator getInstance(Class<T> builderClass) {
    try {
      return defaultObjectCreatorMap.get(builderClass).newInstance();
    } catch (InstantiationException | IllegalAccessException e) {
      throw new IllegalStateException(e);
    }
  }
}
