package software.wings.api.pcf;

import software.wings.api.DeploymentInfo;
import software.wings.api.PcfDeploymentInfo;
import software.wings.helpers.ext.pcf.request.PcfRouteUpdateRequestConfigData;
import software.wings.helpers.ext.pcf.response.PcfAppSetupTimeDetails;
import software.wings.service.impl.instance.DeploymentInfoExtractor;
import software.wings.sm.StepExecutionSummary;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class PcfRouteSwapExecutionSummary extends StepExecutionSummary implements DeploymentInfoExtractor {
  private String organization;
  private String space;
  private PcfRouteUpdateRequestConfigData pcfRouteUpdateRequestConfigData;

  @Override
  public Optional<List<DeploymentInfo>> extractDeploymentInfo() {
    if (pcfRouteUpdateRequestConfigData.isDownsizeOldApplication()) {
      List<DeploymentInfo> pcfDeploymentInfo = new ArrayList<>();
      List<PcfAppSetupTimeDetails> details = pcfRouteUpdateRequestConfigData.getExistingApplicationDetails();
      details.forEach(existingApp
          -> pcfDeploymentInfo.add(PcfDeploymentInfo.builder()
                                       .applicationName(existingApp.getApplicationName())
                                       .applicationGuild(existingApp.getApplicationGuid())
                                       .build()));
      return Optional.of(pcfDeploymentInfo);
    }
    return Optional.empty();
  }
}
