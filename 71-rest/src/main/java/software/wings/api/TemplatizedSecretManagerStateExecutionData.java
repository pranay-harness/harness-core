package software.wings.api;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.sm.StateExecutionData;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class TemplatizedSecretManagerStateExecutionData extends StateExecutionData {
  private String kmsId;
  private String workflowExecutionId;
}
