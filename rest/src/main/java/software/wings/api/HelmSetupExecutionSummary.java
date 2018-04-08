package software.wings.api;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.sm.StepExecutionSummary;

/**
 * Created by anubhaw on 4/3/18.
 */
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class HelmSetupExecutionSummary extends StepExecutionSummary {
  private String releaseName;
  private String oldVersion;
}
