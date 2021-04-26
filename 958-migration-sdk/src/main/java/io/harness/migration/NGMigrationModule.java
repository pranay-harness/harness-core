package io.harness.migration;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.migration.service.NGMigrationService;
import io.harness.migration.service.impl.NGMigrationServiceImpl;

import com.google.inject.AbstractModule;
import java.util.concurrent.atomic.AtomicReference;

@OwnedBy(DX)
public class NGMigrationModule extends AbstractModule {
  private static final AtomicReference<NGMigrationModule> instanceRef = new AtomicReference();

  public NGMigrationModule() {}

  @Override
  protected void configure() {
    bind(NGMigrationService.class).to(NGMigrationServiceImpl.class);
  }

  public static NGMigrationModule getInstance() {
    if (instanceRef.get() == null) {
      instanceRef.compareAndSet(null, new NGMigrationModule());
    }
    return instanceRef.get();
  }
}
