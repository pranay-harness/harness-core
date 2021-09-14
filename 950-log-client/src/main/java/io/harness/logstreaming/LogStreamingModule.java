/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
