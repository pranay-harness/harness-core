package software.wings.api.k8s;

import static io.harness.annotations.dev.HarnessModule._870_CG_ORCHESTRATION;
import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.helpers.ext.helm.response.HelmChartInfo;
import software.wings.sm.StepExecutionSummary;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@TargetModule(_870_CG_ORCHESTRATION)
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
