package software.wings.api.pcf;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.sm.StepExecutionSummary;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class PcfDeployExecutionSummary extends StepExecutionSummary {
  private String releaseName;
  private List<PcfServiceData> instaceData;

  public PcfDeployContextElement getPcfDeployContextForRollback() {
    return PcfDeployContextElement.builder().instanceData(instaceData).name(releaseName).build();
  }
}
