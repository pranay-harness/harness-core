package io.harness.cdng;

import com.google.inject.AbstractModule;

import io.harness.cdng.core.services.api.ProjectService;
import io.harness.cdng.core.services.impl.ProjectServiceImpl;
import io.harness.mongo.MongoPersistence;
import io.harness.persistence.HPersistence;

public class CDNextGenModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(HPersistence.class).to(MongoPersistence.class);
    bind(ProjectService.class).to(ProjectServiceImpl.class);
  }
}
