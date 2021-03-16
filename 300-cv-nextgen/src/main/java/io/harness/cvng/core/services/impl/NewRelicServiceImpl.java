package io.harness.cvng.core.services.impl;

import io.harness.cvng.core.services.api.NewRelicService;

import java.util.Arrays;
import java.util.List;

public class NewRelicServiceImpl implements NewRelicService {
  private static final List<String> NEW_RELIC_ENDPOINTS =
      Arrays.asList("https://insights-api.newrelic.com/", "https://insights-api.eu.newrelic.com/");

  @Override
  public List<String> getNewRelicEndpoints() {
    return NEW_RELIC_ENDPOINTS;
  }
}
