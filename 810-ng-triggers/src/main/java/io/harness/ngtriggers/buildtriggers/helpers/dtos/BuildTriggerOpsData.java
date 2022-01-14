package io.harness.ngtriggers.buildtriggers.helpers.dtos;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.dto.TriggerDetails;

import com.google.inject.Inject;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@OwnedBy(PIPELINE)
public class BuildTriggerOpsData {
  Map<String, Object> triggerSpecMap;
  Map<String, Object> pipelineBuildSpecMap;
  TriggerDetails triggerDetails;
}
