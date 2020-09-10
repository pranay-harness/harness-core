package software.wings.api;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.sm.StateExecutionData;

@OwnedBy(PL)
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class TemplatizedSecretManagerStateExecutionData extends StateExecutionData {
  private String kmsId;
  private String workflowExecutionId;
}
