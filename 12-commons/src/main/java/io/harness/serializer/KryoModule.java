package io.harness.serializer;

import io.harness.govern.DependencyModule;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

@Slf4j
public class KryoModule extends DependencyModule {
  private static volatile KryoModule instance;

  public static KryoModule getInstance() {
    if (instance == null) {
      instance = new KryoModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    // Dummy kryo initialization trigger to make sure it is in good condition
    KryoUtils.asBytes(1);
  }

  @Override
  public Set<DependencyModule> dependencies() {
    return null;
  }
}
