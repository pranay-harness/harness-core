package software.wings.api.k8s;

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
@EqualsAndHashCode(callSuper = true)
public class K8sExecutionSummary extends StepExecutionSummary {
  private String namespace;
  private String releaseName;
  private Integer releaseNumber;
  private Integer targetInstances;
  private Set<String> namespaces;
  private HelmChartInfo helmChartInfo;
  private String blueGreenStageColor;
}
