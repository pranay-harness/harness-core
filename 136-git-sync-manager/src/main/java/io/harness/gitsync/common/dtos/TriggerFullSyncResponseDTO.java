
package io.harness.gitsync.common.dtos;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(name = "TriggerFullSyncResponse", description = "This has details to trigger Full Sync")
@OwnedBy(DX)
public class TriggerFullSyncResponseDTO {
  @Schema(description = "Indicates whether Full Sync is triggered or not") Boolean isFullSyncTriggered;
}
