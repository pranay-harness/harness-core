/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.gitsync.core.dtos;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.Trimmed;
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
public class YamlChangeSetDTO {
  @NotEmpty String changesetId;
  @Trimmed @NotEmpty @NotNull String accountId;
  @NotNull String status;
  // todo: replace/modify with whatever comes from webhook svc
  @Valid GitWebhookRequestAttributes gitWebhookRequestAttributes;
  @NotNull YamlChangeSetEventType eventType;
  @NotNull String repoUrl;
  @NotNull String branch;
  long retryCount;

  // Any special event metadata which has to go back from queue as is can be pushed in this interface.
  @Valid EventMetadata eventMetadata;
}
