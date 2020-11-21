package software.wings.api;

import software.wings.sm.StepExecutionSummary;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

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
  private String namespace;
  private String commandFlags;

  @Builder
  public HelmSetupExecutionSummary(String releaseName, Integer prevVersion, Integer newVersion, Integer rollbackVersion,
      String namespace, String commandFlags) {
    this.releaseName = releaseName;
    this.prevVersion = prevVersion;
    this.newVersion = newVersion;
    this.rollbackVersion = rollbackVersion;
    this.namespace = namespace;
    this.commandFlags = commandFlags;
  }
}
