package io.harness.gitsync.core.dtos;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.common.beans.EventMetadata;
import io.harness.gitsync.common.beans.YamlChangeSetEventType;
import io.harness.gitsync.core.beans.GitWebhookRequestAttributes;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

@Value
@Builder
@OwnedBy(DX)
public class YamlChangeSetSaveDTO {
  @NotEmpty @NotNull String accountId;
  GitWebhookRequestAttributes gitWebhookRequestAttributes;
  @NotNull YamlChangeSetEventType eventType;
  @NotNull @NotEmpty String repoUrl;
  @NotNull @NotEmpty String branch;

  @Valid EventMetadata eventMetadata;
}
