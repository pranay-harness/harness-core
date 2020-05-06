package io.harness.beans.steps;

import lombok.Builder;
import lombok.Data;
import lombok.Value;

/**
 * CI Step which stores state parameters and metadata for creating advisers and facilitators
 */

@Data
@Value
@Builder
public class CIStep implements Step {
  private CIStepInfo ciStepInfo;
  private CIStepMetadata ciStepMetadata;
}
