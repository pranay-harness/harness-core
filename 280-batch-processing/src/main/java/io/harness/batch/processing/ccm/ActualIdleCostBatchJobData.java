package io.harness.batch.processing.ccm;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ActualIdleCostBatchJobData {
  List<ActualIdleCostData> nodeData;
  List<ActualIdleCostData> podData;
}
