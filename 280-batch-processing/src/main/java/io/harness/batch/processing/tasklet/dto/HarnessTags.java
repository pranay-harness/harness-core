package io.harness.batch.processing.tasklet.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HarnessTags {
  private String key;
  private String value;
}
