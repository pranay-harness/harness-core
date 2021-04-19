package io.harness.batch.processing.config;

import io.harness.timescaledb.DSLContextService;
import io.harness.timescaledb.TimeScaleDBConfig;
import io.harness.timescaledb.TimeScaleDBService;
import io.harness.timescaledb.TimeScaleDBServiceImpl;

import com.google.inject.AbstractModule;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;

@Slf4j
public class BatchProcessingTimescaleModule extends AbstractModule {
  private TimeScaleDBConfig configuration;

  public BatchProcessingTimescaleModule(TimeScaleDBConfig configuration) {
    this.configuration = configuration;
  }

  @Override
  protected void configure() {
    bind(TimeScaleDBService.class).toInstance(new TimeScaleDBServiceImpl(configuration));
    bind(DSLContext.class).toInstance(new DSLContextService(configuration).getDefaultDSLContext());
  }
}
