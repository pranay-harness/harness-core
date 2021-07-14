package io.harness.batch.processing.config;

import io.harness.timescaledb.JooqModule;
import io.harness.timescaledb.TimeScaleDBConfig;
import io.harness.timescaledb.TimeScaleDBService;
import io.harness.timescaledb.TimeScaleDBServiceImpl;
import io.harness.timescaledb.metrics.HExecuteListener;
import io.harness.timescaledb.metrics.QueryStatsPrinter;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;
import org.jooq.ExecuteListener;

@Slf4j
public class BatchProcessingTimescaleModule extends AbstractModule {
  private final TimeScaleDBConfig configuration;
  private static final long slowQuerySeconds = 2;
  private static final long extremelySlowQuerySeconds = 5;

  public BatchProcessingTimescaleModule(TimeScaleDBConfig configuration) {
    this.configuration = configuration;
  }

  @Provides
  @Singleton
  @Named("TimeScaleDBConfig")
  TimeScaleDBConfig timeScaleDBConfig() {
    return configuration;
  }

  @Provides
  @Singleton
  @Named("PSQLExecuteListener")
  ExecuteListener executeListener() {
    return HExecuteListener.getInstance(slowQuerySeconds, extremelySlowQuerySeconds);
  }

  @Override
  protected void configure() {
    bind(TimeScaleDBService.class).toInstance(new TimeScaleDBServiceImpl(configuration));
    install(JooqModule.getInstance());
    bind(QueryStatsPrinter.class).toInstance(HExecuteListener.getInstance(slowQuerySeconds, extremelySlowQuerySeconds));
  }
}
