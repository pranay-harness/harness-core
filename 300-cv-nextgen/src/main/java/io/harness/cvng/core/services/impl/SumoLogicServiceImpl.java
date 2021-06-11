package io.harness.cvng.core.services.impl;

import io.harness.cvng.core.services.api.SumoLogicService;

import java.util.Arrays;
import java.util.List;

public class SumoLogicServiceImpl implements SumoLogicService {
  private static final List<String> SUMO_LOGIC_ENDPOINTS = Arrays.asList("https://api.us2.sumologic.com/api/v1/",
      "https://api.sumologic.com/api/v1/", "https://api.in.sumologic.com/api/v1/",
      "https://api.jp.sumologic.com/api/v1/", "https://api.fed.sumologic.com/api/v1/",
      "https://api.eu.sumologic.com/api/v1/", "https://api.de.sumologic.com/api/v1/",
      "https://api.ca.sumologic.com/api/v1/", "https://api.au.sumologic.com/api/v1/");

  @Override
  public List<String> getSumoLogicEndpoints() {
    return SUMO_LOGIC_ENDPOINTS;
  }
}
