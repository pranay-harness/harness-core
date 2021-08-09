package io.harness.ng.core.entitysetupusage;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.entitysetupusage.impl.EntitySetupUsageServiceImpl;
import io.harness.ng.core.entitysetupusage.service.EntitySetupUsageService;
import io.harness.persistence.HPersistence;

import com.google.inject.AbstractModule;
import java.util.concurrent.atomic.AtomicReference;

@OwnedBy(DX)
public class EntitySetupUsageModule extends AbstractModule {
  private static final AtomicReference<EntitySetupUsageModule> instanceRef = new AtomicReference();

  public EntitySetupUsageModule() {}

  @Override
  protected void configure() {
    registerRequiredBindings();
    bind(EntitySetupUsageService.class).to(EntitySetupUsageServiceImpl.class);
  }

  public static EntitySetupUsageModule getInstance() {
    if (instanceRef.get() == null) {
      instanceRef.compareAndSet((EntitySetupUsageModule) null, new EntitySetupUsageModule());
    }

    return (EntitySetupUsageModule) instanceRef.get();
  }

  private void registerRequiredBindings() {
    requireBinding(HPersistence.class);
  }
}
