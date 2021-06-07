package io.harness.analyserservice;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

import com.google.inject.AbstractModule;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PIPELINE)
@Slf4j
public class AnalyserServiceModule extends AbstractModule {
  private static AnalyserServiceModule instance;
  private final AnalyserServiceConfiguration config;

  private AnalyserServiceModule(AnalyserServiceConfiguration config) {
    this.config = config;
  }

  public static synchronized AnalyserServiceModule getInstance(AnalyserServiceConfiguration config) {
    if (instance == null) {
      instance = new AnalyserServiceModule(config);
    }
    return instance;
  }

  @Override
  protected void configure() {
    install(new EventsFrameworkModule(config.getEventsFrameworkConfiguration()));
  }
}
