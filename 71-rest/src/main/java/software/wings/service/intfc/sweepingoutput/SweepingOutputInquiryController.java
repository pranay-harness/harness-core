package software.wings.service.intfc.sweepingoutput;

import io.harness.data.validator.Trimmed;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import software.wings.api.PhaseElement;
import software.wings.sm.StateExecutionInstance;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

@UtilityClass
@Slf4j
public class SweepingOutputInquiryController {
  public static SweepingOutputInquiry obtainFromStateExecutionInstance(
      @NotNull StateExecutionInstance stateExecutionInstance, @Nullable @Trimmed String namePrefix) {
    String name = stateExecutionInstance.getDisplayName().trim();
    if (namePrefix != null) {
      name = namePrefix + name;
    }
    return SweepingOutputInquiry.builder()
        .appId(stateExecutionInstance.getAppId())
        .name(name)
        .workflowExecutionId(stateExecutionInstance.getExecutionUuid())
        .stateExecutionId(stateExecutionInstance.getUuid())
        .phaseExecutionId(getPhaseExecutionId(stateExecutionInstance))
        .build();
  }

  public static SweepingOutputInquiry obtainFromStateExecutionInstanceWithoutName(
      @NotNull StateExecutionInstance stateExecutionInstance) {
    return SweepingOutputInquiry.builder()
        .appId(stateExecutionInstance.getAppId())
        .workflowExecutionId(stateExecutionInstance.getExecutionUuid())
        .stateExecutionId(stateExecutionInstance.getUuid())
        .phaseExecutionId(getPhaseExecutionId(stateExecutionInstance))
        .build();
  }

  @Nullable
  private static String getPhaseExecutionId(@NotNull StateExecutionInstance stateExecutionInstance) {
    PhaseElement phaseElement = stateExecutionInstance.fetchPhaseElement();
    return phaseElement == null
        ? null
        : stateExecutionInstance.getExecutionUuid() + phaseElement.getUuid() + phaseElement.getPhaseName();
  }
}
