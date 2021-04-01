package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.HelmCommandFlagConstants.HelmSubCommand;

import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(CDP)
public class HelmCommandFlagConfig {
  @NotNull private Map<HelmSubCommand, String> valueMap;
}
