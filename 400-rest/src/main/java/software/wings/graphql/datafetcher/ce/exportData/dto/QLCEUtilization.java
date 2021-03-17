package software.wings.graphql.datafetcher.ce.exportData.dto;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
@TargetModule(Module._380_CG_GRAPHQL)
public enum QLCEUtilization {
  CPU_REQUEST,
  CPU_LIMIT,
  CPU_UTILIZATION_VALUE,
  MEMORY_REQUEST,
  MEMORY_LIMIT,
  MEMORY_UTILIZATION_VALUE
}
