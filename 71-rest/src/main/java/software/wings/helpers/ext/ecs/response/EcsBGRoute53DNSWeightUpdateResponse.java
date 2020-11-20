package software.wings.helpers.ext.ecs.response;

import io.harness.logging.CommandExecutionStatus;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class EcsBGRoute53DNSWeightUpdateResponse extends EcsCommandResponse {
  @Builder
  public EcsBGRoute53DNSWeightUpdateResponse(CommandExecutionStatus commandExecutionStatus, String output) {
    super(commandExecutionStatus, output);
  }
}
