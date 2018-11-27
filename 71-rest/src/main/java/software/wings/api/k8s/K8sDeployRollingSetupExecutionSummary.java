package software.wings.api.k8s;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.sm.StepExecutionSummary;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class K8sDeployRollingSetupExecutionSummary extends StepExecutionSummary {
  private String releaseName;
  private Integer releaseNumber;
}
