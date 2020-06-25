package software.wings.api.k8s;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.helpers.ext.helm.response.HelmChartInfo;
import software.wings.sm.StepExecutionSummary;

import java.util.Set;

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
