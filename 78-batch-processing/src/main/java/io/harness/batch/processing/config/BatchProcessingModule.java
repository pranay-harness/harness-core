package io.harness.batch.processing.config;

import com.google.inject.AbstractModule;

import io.harness.mongo.MongoPersistence;
import io.harness.persistence.HPersistence;

public class BatchProcessingModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(HPersistence.class).to(MongoPersistence.class);
  }
}
