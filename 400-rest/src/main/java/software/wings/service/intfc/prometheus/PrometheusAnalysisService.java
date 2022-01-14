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
