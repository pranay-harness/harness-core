/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
