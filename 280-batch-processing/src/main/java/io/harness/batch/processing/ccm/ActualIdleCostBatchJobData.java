package io.harness.batch.processing.ccm;

import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ActualIdleCostBatchJobData {
  List<ActualIdleCostData> nodeData;
  List<ActualIdleCostData> podData;
}
