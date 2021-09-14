/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.services.impl;

import io.harness.cvng.core.services.api.SumoLogicService;

import java.util.Arrays;
import java.util.List;

public class SumoLogicServiceImpl implements SumoLogicService {
  private static final List<String> SUMO_LOGIC_ENDPOINTS =
      Arrays.asList("https://api.us2.sumologic.com/", "https://api.sumologic.com/", "https://api.in.sumologic.com/",
          "https://api.jp.sumologic.com/", "https://api.fed.sumologic.com/", "https://api.eu.sumologic.com/",
          "https://api.de.sumologic.com/", "https://api.ca.sumologic.com/", "https://api.au.sumologic.com/");

  @Override
  public List<String> getSumoLogicEndpoints() {
    return SUMO_LOGIC_ENDPOINTS;
  }
}
