package io.harness.tracing;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import io.harness.eventsframework.api.Producer;

import javax.inject.Named;

public abstract class AbstractPersistenceTracerModule extends AbstractModule {

  @Override
  protected void configure() {
    install(PersistenceTracerModule.getInstance());
  }

  @Provides
  @Named(PersistenceTracerConstants.QUERY_ANALYSIS_PRODUCER)
  public Producer obtainProducer(){
    return producerProvider();
  }

  public abstract Producer producerProvider();

}
