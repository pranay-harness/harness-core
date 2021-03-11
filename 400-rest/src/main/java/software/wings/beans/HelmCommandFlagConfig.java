package software.wings.beans;

import software.wings.beans.HelmCommandFlagConstants.HelmSubCommand;

import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HelmCommandFlagConfig {
  @NotNull private Map<HelmSubCommand, String> valueMap;
}
