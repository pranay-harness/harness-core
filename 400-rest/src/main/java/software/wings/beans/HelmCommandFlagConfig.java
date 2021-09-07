package software.wings.beans;

import static io.harness.annotations.dev.HarnessModule._957_CG_BEANS;
import static io.harness.annotations.dev.HarnessTeam.CDP;

import static java.util.Collections.unmodifiableMap;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.HelmCommandFlagConstants.HelmSubCommand;

import java.util.Map;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(CDP)
@TargetModule(_957_CG_BEANS)
public class HelmCommandFlagConfig {
  @NotNull private Map<HelmSubCommand, String> valueMap;

  @Nullable
  public static HelmCommandFlagConfig cloneFrom(@Nullable HelmCommandFlagConfig sourceConfig) {
    if (sourceConfig == null) {
      return null;
    }

    return HelmCommandFlagConfig.builder().valueMap(unmodifiableMap(sourceConfig.getValueMap())).build();
  }
}
