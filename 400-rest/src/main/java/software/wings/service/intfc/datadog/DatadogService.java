package software.wings.service.intfc.datadog;

import java.util.Map;

public interface DatadogService {
  String getConcatenatedListOfMetricsForValidation(String defaultMetrics, Map<String, String> dockerMetrics,
      Map<String, String> kubernetesMetrics, Map<String, String> ecsMetrics);
}
