package io.harness;

import io.harness.ngtriggers.service.NGTriggerService;
import io.harness.ngtriggers.service.impl.NGTriggerServiceImpl;

import com.google.inject.AbstractModule;
import java.util.concurrent.atomic.AtomicReference;

public class NGTriggersModule extends AbstractModule {
  private static final AtomicReference<NGTriggersModule> instanceRef = new AtomicReference<>();

  public static NGTriggersModule getInstance() {
    if (instanceRef.get() == null) {
      instanceRef.compareAndSet(null, new NGTriggersModule());
    }
    return instanceRef.get();
  }

  @Override
  protected void configure() {
    bind(NGTriggerService.class).to(NGTriggerServiceImpl.class);
  }
}
