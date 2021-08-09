package io.harness.pms.inputset.gitsync;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.beans.YamlDTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.Valid;
import lombok.Builder;
import lombok.Value;

@OwnedBy(PIPELINE)
@Value
@Builder
public class InputSetYamlDTO implements YamlDTO {
  @JsonProperty("inputSet") @Valid InputSetYamlInfoDTO inputSetInfo;
  @JsonProperty("overlayInputSet") @Valid OverlayInputSetYamlInfoDTO overlayInputSetInfo;
}
