package io.harness;

import com.google.inject.AbstractModule;

public class PipelineServiceModule extends AbstractModule {
  private final PipelineServiceConfiguration appConfig;

  public PipelineServiceModule(PipelineServiceConfiguration appConfig) {
    this.appConfig = appConfig;
  }

  @Override
  protected void configure() {}
}
