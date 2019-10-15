package io.harness.batch.processing.config;

import com.google.inject.AbstractModule;

import io.harness.timescaledb.TimeScaleDBConfig;
import io.harness.timescaledb.TimeScaleDBService;
import io.harness.timescaledb.TimeScaleDBServiceImpl;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BatchProcessingTimescaleModule extends AbstractModule {
  private TimeScaleDBConfig configuration;

  public BatchProcessingTimescaleModule(TimeScaleDBConfig configuration) {
    this.configuration = configuration;
  }

  @Override
  protected void configure() {
    bind(TimeScaleDBService.class).toInstance(new TimeScaleDBServiceImpl(configuration));
  }
}
