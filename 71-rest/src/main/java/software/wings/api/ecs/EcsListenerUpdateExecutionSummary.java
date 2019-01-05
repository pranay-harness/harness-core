package software.wings.api.ecs;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.helpers.ext.ecs.request.EcsListenerUpdateRequestConfigData;
import software.wings.sm.StepExecutionSummary;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class EcsListenerUpdateExecutionSummary extends StepExecutionSummary {
  private EcsListenerUpdateRequestConfigData updateRequestConfigData;
}
