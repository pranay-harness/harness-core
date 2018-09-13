package software.wings.api.pcf;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.wings.beans.ResizeStrategy;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest;
import software.wings.helpers.ext.pcf.response.PcfAppSetupTimeDetails;
import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;
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

  @Override
  public ContextElementType getElementType() {
    return ContextElementType.PCF_SERVICE_SETUP;
  }

  @Override
  public Map<String, Object> paramMap(ExecutionContext context) {
    return null;
  }

  @Override
  public ContextElement cloneMin() {
    return null;
  }
}
