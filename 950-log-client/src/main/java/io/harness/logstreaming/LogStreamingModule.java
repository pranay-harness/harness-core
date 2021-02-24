package io.harness.logstreaming;

import com.google.inject.AbstractModule;

public class LogStreamingModule extends AbstractModule {
  private String logStreamingServiceBaseUrl;

  public LogStreamingModule(String logStreamingServiceBaseUrl) {
    this.logStreamingServiceBaseUrl = logStreamingServiceBaseUrl;
  }

  @Override
  protected void configure() {
    bind(LogStreamingClient.class).toProvider(new LogStreamingClientFactory(logStreamingServiceBaseUrl));
  }
}
