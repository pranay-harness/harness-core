/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.analysis;

import static io.harness.persistence.HQuery.excludeAuthority;

import software.wings.dl.WingsPersistence;
import software.wings.service.impl.analysis.ExperimentalMetricAnalysisRecord.ExperimentalMetricAnalysisRecordKeys;
import software.wings.service.impl.analysis.MetricAnalysisRecord.MetricAnalysisRecordKeys;
import software.wings.service.intfc.analysis.ExperimentalMetricAnalysisRecordService;

import com.google.inject.Inject;
import org.mongodb.morphia.query.Sort;

public class ExperimentalMetricAnalysisRecordServiceImpl implements ExperimentalMetricAnalysisRecordService {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public ExperimentalMetricAnalysisRecord getLastAnalysisRecord(String stateExecutionId, String experimentName) {
    return wingsPersistence.createQuery(ExperimentalMetricAnalysisRecord.class, excludeAuthority)
        .filter(MetricAnalysisRecordKeys.stateExecutionId, stateExecutionId)
        .order(Sort.descending(MetricAnalysisRecordKeys.analysisMinute))
        .filter(ExperimentalMetricAnalysisRecordKeys.experimentName, experimentName)
        .get();
  }

  @Override
  public ExperimentalMetricAnalysisRecord getAnalysisRecordForMinute(String stateExecutionId, Integer analysisMinute) {
    return wingsPersistence.createQuery(ExperimentalMetricAnalysisRecord.class)
        .filter(MetricAnalysisRecordKeys.stateExecutionId, stateExecutionId)
        .filter(MetricAnalysisRecordKeys.analysisMinute, analysisMinute)
        .get();
  }
}
