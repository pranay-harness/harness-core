package io.harness.execution.export.metadata;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;
import software.wings.api.ExecutionDataValue;

import java.util.Map;

@OwnedBy(CDC)
@Value
@Builder
public class InstanceCountMetadata {
  Integer total;
  Integer succeeded;
  Integer failed;

  static InstanceCountMetadata extractFromExecutionDetails(Map<String, ExecutionDataValue> executionDetailsMap) {
    if (isEmpty(executionDetailsMap)) {
      return null;
    }

    Integer total = countFromExecutionDetails(executionDetailsMap, "Total instances");
    if (total == null || total <= 0) {
      return null;
    }

    Integer succeeded = countFromExecutionDetails(executionDetailsMap, "Succeeded");
    Integer failed = countFromExecutionDetails(executionDetailsMap, "Error");
    return InstanceCountMetadata.builder().total(total).succeeded(succeeded).failed(failed).build();
  }

  private static Integer countFromExecutionDetails(Map<String, ExecutionDataValue> executionDetailsMap, String key) {
    if (!executionDetailsMap.containsKey(key)) {
      return null;
    }

    ExecutionDataValue dataValue = executionDetailsMap.get(key);
    executionDetailsMap.remove(key);
    Object value = dataValue.getValue();
    if (value instanceof Integer) {
      Integer intValue = (Integer) value;
      return intValue < 0 ? null : intValue;
    }

    return null;
  }
}
