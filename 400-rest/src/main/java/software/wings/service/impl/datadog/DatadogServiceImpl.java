package software.wings.service.impl.datadog;

import static io.harness.data.structure.HasPredicate.hasNone;
import static io.harness.data.structure.HasPredicate.hasSome;
import static io.harness.k8s.manifest.ObjectYamlUtils.encodeDot;

import io.harness.serializer.JsonUtils;

import software.wings.service.intfc.datadog.DatadogService;
import software.wings.sm.states.DatadogState;

import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class DatadogServiceImpl implements DatadogService {
  private static final String COMMA_STR = ",";

  @Override
  public String getConcatenatedListOfMetricsForValidation(String defaultMetrics, Map<String, String> dockerMetrics,
      Map<String, String> kubernetesMetrics, Map<String, String> ecsMetrics) {
    String metricsString = defaultMetrics != null ? defaultMetrics : "";
    if (hasSome(dockerMetrics)) {
      metricsString += String.join(COMMA_STR, dockerMetrics.values());
    }
    if (hasSome(ecsMetrics)) {
      metricsString += String.join(COMMA_STR, ecsMetrics.values());
    }
    if (hasSome(kubernetesMetrics)) {
      metricsString += String.join(COMMA_STR, kubernetesMetrics.values());
    }
    return metricsString;
  }

  /**
   * This method will validate if there are any setups with the same metricName in default metrics and custom metrics
   * @param customMetrics
   * @param metrics
   * @return
   */
  public static Map<String, String> validateNameClashInCustomMetrics(
      Map<String, Set<DatadogState.Metric>> customMetrics, String metrics) {
    Map<String, String> validateFields = new HashMap<>();
    if (hasNone(customMetrics) || hasNone(metrics)) {
      return new HashMap<>();
    }
    List<String> metricList = Arrays.asList(metrics.split(","));
    customMetrics.forEach((hostField, metricSet) -> {
      List<DatadogState.Metric> customMetricList = new ArrayList<>();
      for (Object metricObj : metricSet) {
        DatadogState.Metric metric = JsonUtils.asObject(JsonUtils.asJson(metricObj), DatadogState.Metric.class);
        customMetricList.add(metric);
      }
      customMetricList.forEach(customMetricDefinition -> {
        if (metricList.contains(customMetricDefinition.getMetricName())) {
          validateFields.put("Duplicated metric in custom metric definition for metric: "
                  + encodeDot(customMetricDefinition.getMetricName()),
              "Present in both custom definition and out-of-the-box metrics");
        }
      });
    });
    return validateFields;
  }
}
