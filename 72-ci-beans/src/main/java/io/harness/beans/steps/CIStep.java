package io.harness.beans.steps;

import lombok.Builder;
import lombok.Data;
import lombok.Value;

import javax.validation.constraints.NotNull;

/**
 * CI Step which stores state parameters and metadata for creating advisers and facilitators
 */

@Data
@Value
@Builder
public class CIStep implements Step {
  @NotNull private StepInfo stepInfo;
  private StepMetadata stepMetadata;
}
