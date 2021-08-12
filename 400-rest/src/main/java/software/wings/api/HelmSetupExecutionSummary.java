package software.wings.api;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.sm.StepExecutionSummary;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Created by anubhaw on 4/3/18.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@OwnedBy(CDP)
@TargetModule(HarnessModule._957_CG_BEANS)
public class HelmSetupExecutionSummary extends StepExecutionSummary {
  private String releaseName;
  private Integer prevVersion;
  private Integer newVersion;
  private Integer rollbackVersion;
  @Deprecated private String namespace;
  private String commandFlags;
  private List<String> namespaces;

  @Builder
  public HelmSetupExecutionSummary(String releaseName, Integer prevVersion, Integer newVersion, Integer rollbackVersion,
      String namespace, String commandFlags, List<String> namespaces) {
    this.releaseName = releaseName;
    this.prevVersion = prevVersion;
    this.newVersion = newVersion;
    this.rollbackVersion = rollbackVersion;
    this.namespace = namespace;
    this.commandFlags = commandFlags;
    this.namespaces = namespaces;
  }
}
