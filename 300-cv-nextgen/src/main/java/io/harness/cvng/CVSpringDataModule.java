package io.harness.cvng;

import io.harness.cvng.core.services.api.DocumentOneService;
import io.harness.cvng.core.services.impl.DocumentOneServiceImpl;
import io.harness.persistence.HPersistence;

import com.google.inject.AbstractModule;

public class CVSpringDataModule extends AbstractModule {
  @Override
  protected void configure() {
    registerRequiredBindings();
    bind(DocumentOneService.class).to(DocumentOneServiceImpl.class);
  }

  private void registerRequiredBindings() {
    requireBinding(HPersistence.class);
  }
}
