package software.wings.api.pcf;

import com.google.common.collect.Maps;

import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.task.protocol.ResponseData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.api.ExecutionDataValue;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest;
import software.wings.helpers.ext.pcf.request.PcfRouteUpdateRequestConfigData;
import software.wings.sm.StateExecutionData;

import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PcfRouteUpdateStateExecutionData extends StateExecutionData implements ResponseData {
  private String activityId;
  private String accountId;
  private String appId;
  private PcfCommandRequest pcfCommandRequest;
  private String commandName;
  private PcfRouteUpdateRequestConfigData pcfRouteUpdateRequestConfigData;

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    return getInternalExecutionDetails();
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    return getInternalExecutionDetails();
  }

  private Map<String, ExecutionDataValue> getInternalExecutionDetails() {
    Map<String, ExecutionDataValue> execDetails = Maps.newLinkedHashMap();
    putNotNull(execDetails, "organization",
        ExecutionDataValue.builder().value(pcfCommandRequest.getOrganization()).displayName("Organization").build());
    putNotNull(execDetails, "space",
        ExecutionDataValue.builder().value(pcfCommandRequest.getSpace()).displayName("Space").build());
    putNotNull(execDetails, "commandName",
        ExecutionDataValue.builder().value(commandName).displayName("Command Name").build());
    putNotNull(execDetails, "updateConfig",
        ExecutionDataValue.builder().value(getDisplayStringForUpdateConfig()).displayName("Update Config").build());
    // putting activityId is very important, as without it UI wont make call to fetch commandLogs that are shown
    // in activity window
    putNotNull(
        execDetails, "activityId", ExecutionDataValue.builder().value(activityId).displayName("Activity Id").build());

    return execDetails;
  }

  private String getDisplayStringForUpdateConfig() {
    StringBuilder stringBuilder = new StringBuilder(128);

    if (pcfRouteUpdateRequestConfigData.isStandardBlueGreen()) {
      stringBuilder.append('{')
          .append(pcfRouteUpdateRequestConfigData.getNewApplicatiaonName())
          .append(" : ")
          .append(pcfRouteUpdateRequestConfigData.getFinalRoutes())
          .append('}');

      pcfRouteUpdateRequestConfigData.getExistingApplicationNames().forEach(appName
          -> stringBuilder.append(", {")
                 .append(appName)
                 .append(" : ")
                 .append(pcfRouteUpdateRequestConfigData.getTempRoutes())
                 .append('}'));
    } else {
      if (EmptyPredicate.isNotEmpty(pcfRouteUpdateRequestConfigData.getExistingApplicationNames())) {
        pcfRouteUpdateRequestConfigData.getExistingApplicationNames().forEach(appName
            -> stringBuilder.append(appName)
                   .append("[")
                   .append(pcfRouteUpdateRequestConfigData.getFinalRoutes())
                   .append("]"));
      }
    }

    return stringBuilder.toString();
  }

  private String getDisplayString(List<String> inputs) {
    StringBuilder builder = new StringBuilder();
    inputs.forEach(input -> builder.append(input).append(" "));
    return builder.toString();
  }

  @Override
  public PcfRouteSwapExecutionSummary getStepExecutionSummary() {
    return PcfRouteSwapExecutionSummary.builder()
        .organization(pcfCommandRequest.getOrganization())
        .space(pcfCommandRequest.getSpace())
        .pcfRouteUpdateRequestConfigData(pcfRouteUpdateRequestConfigData)
        .build();
  }
}
