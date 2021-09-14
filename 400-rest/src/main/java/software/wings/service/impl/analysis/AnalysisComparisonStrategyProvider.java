/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.analysis;

import software.wings.stencils.DataProvider;

import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by rsingh on 08/16/17©.
 */
@Singleton
public class AnalysisComparisonStrategyProvider implements DataProvider {
  @Override
  public Map<String, String> getData(String appId, Map<String, String> params) {
    final Map<String, String> rv = new HashMap<>();
    rv.put(AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS.name(), "Previous analysis");
    rv.put(AnalysisComparisonStrategy.COMPARE_WITH_CURRENT.name(), "Canary analysis");
    return rv;
  }
}
