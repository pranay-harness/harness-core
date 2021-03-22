package io.harness.ngtriggers.beans.entity.metadata;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CronMetadata {
  String expression;
}
