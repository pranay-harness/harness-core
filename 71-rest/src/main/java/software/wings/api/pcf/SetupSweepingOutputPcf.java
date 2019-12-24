package software.wings.api.pcf;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.beans.SweepingOutput;
import io.harness.delegate.task.pcf.PcfManifestsPackage;
import lombok.Builder;
import lombok.Value;
import software.wings.api.pcf.InfoVariables.InfoVariablesBuilder;
import software.wings.beans.ResizeStrategy;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest;
import software.wings.helpers.ext.pcf.response.PcfAppSetupTimeDetails;

import java.util.List;

@Value
@Builder
public class SetupSweepingOutputPcf implements SweepingOutput {
  public static final String SWEEPING_OUTPUT_NAME = "setupSweepingOutputPcf";

  private String uuid;
  private String serviceId;
  private String infraMappingId;
  private String name;
  private String commandName;
  private Integer maxInstanceCount;
  private boolean useCurrentRunningInstanceCount;
  private Integer currentRunningInstanceCount;
  private ResizeStrategy resizeStrategy;
  private PcfCommandRequest pcfCommandRequest;
  private String ManifestYaml;
  private PcfAppSetupTimeDetails newPcfApplicationDetails;
  private Integer totalPreviousInstanceCount;
  private List<String> tempRouteMap;
  private List<String> routeMaps;
  private Integer timeoutIntervalInMinutes;
  private List<PcfAppSetupTimeDetails> appDetailsToBeDownsized;
  private boolean isStandardBlueGreenWorkflow;
  private boolean isDownsizeOldApps;
  private boolean isUseCfCli;
  private boolean enforceSslValidation;
  private boolean useAppAutoscalar;
  private Integer desiredActualFinalCount;
  private PcfManifestsPackage pcfManifestsPackage;

  public InfoVariables fetchPcfVariableInfo() {
    InfoVariablesBuilder infoVariablesBuilder = InfoVariables.builder();
    if (newPcfApplicationDetails != null) {
      infoVariablesBuilder.newAppName(newPcfApplicationDetails.getApplicationName());
      infoVariablesBuilder.newAppGuid(newPcfApplicationDetails.getApplicationGuid());
      infoVariablesBuilder.newAppRoutes(newPcfApplicationDetails.getUrls());
    }

    PcfAppSetupTimeDetails oldAppDetails = getOldAppDetail(appDetailsToBeDownsized);
    if (oldAppDetails != null) {
      infoVariablesBuilder.oldAppName(oldAppDetails.getApplicationName());
      infoVariablesBuilder.oldAppGuid(oldAppDetails.getApplicationGuid());
      infoVariablesBuilder.oldAppRoutes(oldAppDetails.getUrls());
    }

    infoVariablesBuilder.finalRoutes(routeMaps);
    infoVariablesBuilder.tempRoutes(tempRouteMap);
    return infoVariablesBuilder.build();
  }

  private PcfAppSetupTimeDetails getOldAppDetail(List<PcfAppSetupTimeDetails> appDetailsToBeDownsized) {
    if (isNotEmpty(appDetailsToBeDownsized)) {
      return appDetailsToBeDownsized.get(0);
    }

    return null;
  }
}
