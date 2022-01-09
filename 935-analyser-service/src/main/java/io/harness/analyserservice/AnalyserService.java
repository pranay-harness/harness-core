/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.analyserservice;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.event.QueryAlertCategory;
import io.harness.event.QueryStats;

import java.util.List;
import java.util.Map;

@OwnedBy(HarnessTeam.PIPELINE)
public interface AnalyserService {
  List<QueryStats> getQueryStats(String service, String version);
  Map<String, Integer> getAlertMap(String service, QueryAlertCategory alertCategory);
  List<QueryStats> getMostExpensiveQueries(String service, String version);
  List<QueryStats> getQueryStats(String service, String version, QueryAlertCategory alertCategory);
  List<QueryStats> getDisjointQueries(String service, String oldVersion, String newVersion);
}
