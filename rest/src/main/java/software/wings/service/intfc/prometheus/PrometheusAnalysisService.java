package software.wings.service.intfc.prometheus;

import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.prometheus.PrometheusSetupTestNodeData;

/**
 * Prometheus Analysis Service
 * Created by Pranjal on 09/02/2018
 */
public interface PrometheusAnalysisService {
  VerificationNodeDataSetupResponse getMetricsWithDataForNode(PrometheusSetupTestNodeData setupTestNodeData);
}
