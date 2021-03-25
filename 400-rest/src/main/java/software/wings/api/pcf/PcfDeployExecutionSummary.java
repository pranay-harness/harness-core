package software.wings.api.pcf;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.HasPredicate.hasNone;

import io.harness.annotations.dev.OwnedBy;

import software.wings.api.DeploymentInfo;
import software.wings.api.PcfDeploymentInfo;
import software.wings.service.impl.instance.DeploymentInfoExtractor;
import software.wings.sm.StepExecutionSummary;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@Slf4j
@OwnedBy(CDP)
public class PcfDeployExecutionSummary extends StepExecutionSummary implements DeploymentInfoExtractor {
  private String releaseName;
  private List<PcfServiceData> instaceData;

  @Override
  public Optional<List<DeploymentInfo>> extractDeploymentInfo() {
    if (hasNone(instaceData)) {
      log.warn(
          "Both old and new app resize details are empty. Cannot proceed for phase step for state execution instance");
      return Optional.empty();
    }

    List<DeploymentInfo> pcfDeploymentInfo = new ArrayList<>();
    instaceData.forEach(pcfServiceData
        -> pcfDeploymentInfo.add(PcfDeploymentInfo.builder()
                                     .applicationName(pcfServiceData.getName())
                                     .applicationGuild(pcfServiceData.getId())
                                     .build()));
    return Optional.of(pcfDeploymentInfo);
  }
}
