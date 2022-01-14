package io.harness.ngtriggers.beans.entity.metadata;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.dto.PollingConfig;
import io.harness.ngtriggers.beans.source.NGTriggerType;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(PIPELINE)
public class BuildMetadata {
  NGTriggerType type;
  String buildSourceType;
  PollingConfig pollingConfig;
}
