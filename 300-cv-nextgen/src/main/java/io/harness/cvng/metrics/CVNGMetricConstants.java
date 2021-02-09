package io.harness.cvng.metrics;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PUBLIC)
public class CVNGMetricConstants {
  private CVNGMetricConstants() {}

  static final String ANALYSIS_ORCHESTRATOR_COUNT = "analysis_orchestrator_queue_count";
  static final String LE_TASK_QUEUED_COUNT = "le_task__queued_count";
  static final Map<String, List<String>> metricLabelMap;
  static {
    metricLabelMap = new HashMap<>();
    metricLabelMap.put(ANALYSIS_ORCHESTRATOR_COUNT, Arrays.asList());
  }
}
