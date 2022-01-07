/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.intfc.prometheus;

import software.wings.service.impl.analysis.TimeSeries;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.apm.APMMetricInfo;
import software.wings.service.impl.prometheus.PrometheusSetupTestNodeData;

import java.util.List;
import java.util.Map;

/**
 * Prometheus Analysis Service
 * Created by Pranjal on 09/02/2018
 */
public interface PrometheusAnalysisService {
  VerificationNodeDataSetupResponse getMetricsWithDataForNode(PrometheusSetupTestNodeData setupTestNodeData);
  Map<String, List<APMMetricInfo>> apmMetricEndPointsFetchInfo(List<TimeSeries> timeSeriesInfos);
}
