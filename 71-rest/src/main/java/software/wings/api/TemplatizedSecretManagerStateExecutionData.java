package software.wings.api;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.sm.StateExecutionData;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@OwnedBy(PL)
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class TemplatizedSecretManagerStateExecutionData extends StateExecutionData {
  private String kmsId;
  private String workflowExecutionId;
}
