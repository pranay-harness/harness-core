package software.wings.api;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.sm.StepExecutionSummary;

/**
 * Created by anubhaw on 4/3/18.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class HelmSetupExecutionSummary extends StepExecutionSummary {
  private String releaseName;
  private Integer prevVersion;
  private Integer newVersion;
  private Integer rollbackVersion;

  @Builder
  public HelmSetupExecutionSummary(
      String releaseName, Integer prevVersion, Integer newVersion, Integer rollbackVersion) {
    this.releaseName = releaseName;
    this.prevVersion = prevVersion;
    this.newVersion = newVersion;
    this.rollbackVersion = rollbackVersion;
  }
}
