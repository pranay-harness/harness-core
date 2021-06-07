package io.harness.mongo.tracing;

import io.harness.threading.ThreadPool;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class TracerModule extends AbstractModule {
  private static TracerModule instance;
  public static Module getInstance() {
    if (instance == null) {
      instance = new TracerModule();
    }
    return instance;
  }

  @Override
  public void configure() {}
}
