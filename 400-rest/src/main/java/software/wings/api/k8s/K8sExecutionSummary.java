package software.wings.api.k8s;

import static io.harness.annotations.dev.HarnessModule._871_CG_BEANS;
import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.task.helm.HelmChartInfo;

import software.wings.sm.StepExecutionSummary;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@TargetModule(_871_CG_BEANS)
@OwnedBy(CDP)
@EqualsAndHashCode(callSuper = true)
public class K8sExecutionSummary extends StepExecutionSummary {
  private String namespace;
  private String releaseName;
  private Integer releaseNumber;
  private Integer targetInstances;
  private Set<String> namespaces;
  private HelmChartInfo helmChartInfo;
  private String blueGreenStageColor;
  private Set<String> delegateSelectors;
}
