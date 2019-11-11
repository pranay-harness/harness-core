package software.wings.api.pcf;

import static com.google.common.collect.Maps.newHashMap;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.pcf.model.PcfConstants.CONTEXT_APP_FINAL_ROUTES;
import static io.harness.pcf.model.PcfConstants.CONTEXT_APP_TEMP_ROUTES;
import static io.harness.pcf.model.PcfConstants.CONTEXT_NEW_APP_GUID;
import static io.harness.pcf.model.PcfConstants.CONTEXT_NEW_APP_NAME;
import static io.harness.pcf.model.PcfConstants.CONTEXT_NEW_APP_ROUTES;
import static io.harness.pcf.model.PcfConstants.CONTEXT_OLD_APP_GUID;
import static io.harness.pcf.model.PcfConstants.CONTEXT_OLD_APP_NAME;
import static io.harness.pcf.model.PcfConstants.CONTEXT_OLD_APP_ROUTES;

import com.google.common.collect.ImmutableMap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.context.ContextElementType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.wings.beans.ResizeStrategy;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest;
import software.wings.helpers.ext.pcf.response.PcfAppSetupTimeDetails;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;

import java.util.List;
import java.util.Map;

/**
 * This class will become part of executionContext and will be fetched and refereed by all subsequent phase
 * to get some common details, like newAppName, totalPreviousInstanceCount before deployment started
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PcfSetupContextElement implements ContextElement {
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
  private boolean enforceSslValidation;
  private boolean useAppAutoscalar;

  @Override
  public ContextElementType getElementType() {
    return ContextElementType.PCF_SERVICE_SETUP;
  }

  @Override
  public Map<String, Object> paramMap(ExecutionContext context) {
    Map<String, Object> map = newHashMap();
    if (newPcfApplicationDetails != null) {
      map.put(CONTEXT_NEW_APP_NAME, newPcfApplicationDetails.getApplicationName());
      map.put(CONTEXT_NEW_APP_GUID, newPcfApplicationDetails.getApplicationGuid());
      map.put(CONTEXT_NEW_APP_ROUTES, newPcfApplicationDetails.getUrls());
    }

    PcfAppSetupTimeDetails oldAppDetails = getOldAppDetail(appDetailsToBeDownsized);
    if (oldAppDetails != null) {
      map.put(CONTEXT_OLD_APP_NAME, oldAppDetails.getApplicationName());
      map.put(CONTEXT_OLD_APP_GUID, oldAppDetails.getApplicationGuid());
      map.put(CONTEXT_OLD_APP_ROUTES, oldAppDetails.getUrls());
    }

    map.put(CONTEXT_APP_FINAL_ROUTES, routeMaps);
    map.put(CONTEXT_APP_TEMP_ROUTES, tempRouteMap);
    return ImmutableMap.of("pcf", map);
  }

  private PcfAppSetupTimeDetails getOldAppDetail(List<PcfAppSetupTimeDetails> appDetailsToBeDownsized) {
    if (isNotEmpty(appDetailsToBeDownsized)) {
      return appDetailsToBeDownsized.get(0);
    }

    return null;
  }

  @Override
  public ContextElement cloneMin() {
    return null;
  }
}
